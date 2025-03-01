package ca.jolt.injector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import ca.jolt.exceptions.BeanCreationException;
import ca.jolt.exceptions.BeanNotFoundException;
import ca.jolt.exceptions.CircularDependencyException;
import ca.jolt.exceptions.JoltDIException;
import ca.jolt.injector.annotation.JoltBean;
import ca.jolt.injector.annotation.JoltBeanInjection;
import ca.jolt.injector.annotation.PostConstruct;
import ca.jolt.injector.annotation.PreDestroy;
import ca.jolt.injector.type.BeanScope;
import ca.jolt.injector.type.InitializationMode;

/**
 * The BeanRegistry is responsible for registering bean classes (annotated with
 * {@link ca.jolt.injector.annotation.JoltBean JoltBean}) and their associated,
 * instantiating them, performing dependency injection, and managing their
 * lifecycle.
 */
final class BeanRegistry {

    private static final Logger logger = Logger.getLogger(BeanRegistry.class.getName());

    // Mapping from bean name to its class.
    private final Map<String, Class<?>> beanDefinitions = new HashMap<>();
    // Cache for singleton instances.
    private final Map<String, Object> singletonInstances = new ConcurrentHashMap<>();
    // Lookup for singleton instances by type.
    private final Map<Class<?>, Object> typeToInstance = new ConcurrentHashMap<>();
    // Tracks beans in creation (for circular dependency detection).
    private final Set<String> beansInCreation = Collections.synchronizedSet(new HashSet<>());
    // List of managed beans for lifecycle management.
    private final List<Object> managedBeans = new ArrayList<>();

    /**
     * Registers the given bean class. The class must be annotated with
     * {@link JoltBean}.
     *
     * @param beanClass the bean class.
     * @throws JoltDIException if the class is not annotated or if a bean with the
     *                         same name is already registered.
     */
    public void registerBean(Class<?> beanClass) {
        Objects.requireNonNull(beanClass, "Bean class cannot be null");
        JoltBean annotation = beanClass.getAnnotation(JoltBean.class);
        if (annotation == null) {
            throw new JoltDIException("Class " + beanClass.getName() + " is not annotated with @JoltBean");
        }
        String beanName = getBeanName(beanClass, annotation);
        if (beanDefinitions.containsKey(beanName)) {
            throw new JoltDIException("Duplicate bean name: " + beanName);
        }
        beanDefinitions.put(beanName, beanClass);
        logger.info(() -> "Registered bean: " + beanName);
    }

    /**
     * Eagerly instantiates all singleton beans that are configured for EAGER
     * initialization.
     */
    public void initializeEagerBeans() {
        for (Map.Entry<String, Class<?>> entry : beanDefinitions.entrySet()) {
            Class<?> beanClass = entry.getValue();
            JoltBean annotation = beanClass.getAnnotation(JoltBean.class);
            if (annotation.initialization() == InitializationMode.EAGER &&
                    annotation.scope() == BeanScope.SINGLETON) {
                logger.info("Eagerly initializing bean: " + entry.getKey());
                getBean(entry.getKey());
            }
        }
    }

    /**
     * Retrieves a bean by its registered name.
     *
     * @deprecated Use {@link #getBean(String, Class)} instead.
     * 
     * @param name the bean name.
     * @param <T>  the expected bean type.
     * @return the bean instance.
     * @throws BeanNotFoundException if no bean is found with the given name.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public <T> T getBean(String name) {
        Objects.requireNonNull(name, "Bean name cannot be null");

        Object instance = singletonInstances.get(name);
        if (instance != null) {
            return (T) instance;
        }

        Class<?> beanClass = beanDefinitions.get(name);
        if (beanClass == null) {
            throw new BeanNotFoundException("No bean found with name: " + name);
        }

        return (T) createBean(beanClass, name);
    }

    /**
     * Retrieves a bean by its type.
     *
     * @param type the bean class.
     * @param <T>  the expected bean type.
     * @return the bean instance.
     * @throws BeanNotFoundException if no bean is found assignable to the given
     *                               type.
     */
    public <T> T getBean(Class<T> type) {
        Objects.requireNonNull(type, "Bean type cannot be null");

        Object instance = typeToInstance.get(type);
        if (instance != null) {
            return type.cast(instance);
        }

        for (Map.Entry<String, Class<?>> entry : beanDefinitions.entrySet()) {
            if (type.isAssignableFrom(entry.getValue())) {
                logger.info("Creating bean instance for type: " + type.getName() +
                        " using bean name: " + entry.getKey());
                return type.cast(createBean(entry.getValue(), entry.getKey()));
            }
        }

        throw new BeanNotFoundException("No bean found of type: " + type.getName());
    }

    /**
     * Retrieve a bean by its type and name.
     * 
     * @param type the bean class
     * @param name the bean name
     * @param <T>  the expected bean type
     * @return the bean instance
     * @throws BeanNotFoundException if no bean is found assignable to the given
     *                               type
     */
    public <T> T getBean(Class<T> type, String name) throws BeanNotFoundException {
        Objects.requireNonNull(type, "Bean type cannot be null");
        Objects.requireNonNull(name, "Bean name cannot be null");

        // Check if a bean match the given type and name pass to the function.
        for (Map.Entry<String, Class<?>> entry : beanDefinitions.entrySet()) {
            if (type.isAssignableFrom(entry.getValue()) && entry.getKey().equals(name)) {
                logger.info("Creating bean instance for type: " + type.getName() +
                        " using bean name: " + entry.getKey());
                return type.cast(createBean(entry.getValue(), entry.getKey()));
            }
        }
        // If no bean match the given type and name, throw a BeanNotFoundException.
        throw new BeanNotFoundException("No bean found of type: " + type.getName() + " and name: " + name);
    }

    public void shutdown() {
        logger.info("Shutting down BeanRegistry. Invoking @PreDestroy methods.");
        for (Object bean : managedBeans) {
            try {
                invokeLifecycleMethod(bean, PreDestroy.class);
            } catch (Exception e) {
                logger.warning("Failed to invoke @PreDestroy on: " + bean.getClass().getName());
            }
        }
        managedBeans.clear();
        singletonInstances.clear();
        typeToInstance.clear();
        beanDefinitions.clear();
        beansInCreation.clear();
        logger.info("BeanRegistry shutdown complete.");
    }

    private Object createBean(Class<?> beanClass, String beanName) {
        if (!beansInCreation.add(beanName)) {
            throw new CircularDependencyException("Circular dependency detected for bean: " + beanName +
                    ". Creation chain: " + String.join(" -> ", beansInCreation));
        }
        try {
            checkForConstructor(beanClass);
            Object instance = beanClass.getDeclaredConstructor().newInstance();
            JoltBean annotation = beanClass.getAnnotation(JoltBean.class);
            if (annotation != null && annotation.scope() == BeanScope.SINGLETON) {
                singletonInstances.put(beanName, instance);
                typeToInstance.put(beanClass, instance);
                managedBeans.add(instance);
            }
            injectDependencies(instance);
            beansInCreation.remove(beanName);
            invokeLifecycleMethod(instance, PostConstruct.class);
            return instance;
        } catch (Exception e) {
            beansInCreation.remove(beanName);
            throw new BeanCreationException("Failed to create bean: " + beanName + ". caused by: " + e.getMessage(), e);
        }
    }

    private void checkForConstructor(Class<?> beanClass) {
        try {
            beanClass.getConstructor();
        } catch (NoSuchMethodException e) {
            throw new BeanCreationException(
                    "Bean " + beanClass.getName() + " must have a public no-argument constructor", e);
        }
    }

    private void injectDependencies(Object instance) {
        Class<?> clazz = instance.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(JoltBeanInjection.class)) {
                try {
                    var injection = field.getAnnotation(JoltBeanInjection.class);
                    Object dependency = !injection.value().isEmpty() ? getBean(injection.value())
                            : getBean(field.getType());
                    if (dependency == null && injection.required()) {
                        throw new BeanNotFoundException("Required dependency not found for: " +
                                clazz.getName() + "." + field.getName());
                    }
                    field.set(instance, dependency);
                } catch (IllegalAccessException e) {
                    logger.severe("Error injecting dependency for " + clazz.getName() + "." + field.getName());
                    throw new BeanCreationException("Failed to inject dependency for: " +
                            clazz.getName() + "." + field.getName(), e);
                }
            }
        }
    }

    private void invokeLifecycleMethod(Object instance, Class<? extends Annotation> annotationType) {
        Class<?> clazz = instance.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotationType)) {
                try {
                    method.invoke(instance);
                } catch (Exception e) {
                    throw new JoltDIException("Failed to invoke lifecycle method on: " + clazz.getName(), e);
                }
            }
        }
    }

    private String getBeanName(Class<?> beanClass, JoltBean annotation) {
        String value = annotation.value();
        return value.isEmpty() ? Character.toLowerCase(beanClass.getSimpleName().charAt(0))
                + beanClass.getSimpleName().substring(1) : value;
    }
}
