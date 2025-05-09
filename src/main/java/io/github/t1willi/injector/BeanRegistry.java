package io.github.t1willi.injector;

import io.github.t1willi.annotations.Controller;
import io.github.t1willi.exceptions.BeanCreationException;
import io.github.t1willi.exceptions.BeanNotFoundException;
import io.github.t1willi.exceptions.CircularDependencyException;
import io.github.t1willi.exceptions.JoltDIException;
import io.github.t1willi.injector.annotation.Autowire;
import io.github.t1willi.injector.annotation.Bean;
import io.github.t1willi.injector.type.BeanScope;
import io.github.t1willi.injector.type.InitializationMode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

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

/**
 * The BeanRegistry is responsible for registering bean classes (annotated with
 * {@link JoltBean} or {@link Controller}) and their associated,
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
     * {@link JoltBean} or {@link Controller}.
     *
     * @param beanClass the bean class.
     * @throws JoltDIException if the class is not annotated or if a bean with the
     *                         same name is already registered.
     */
    public void registerBean(Class<?> beanClass) {
        Objects.requireNonNull(beanClass, "Bean class cannot be null");
        Bean joltBeanAnnotation = beanClass.getAnnotation(Bean.class);
        Controller controllerAnnotation = beanClass.getAnnotation(Controller.class);
        if (joltBeanAnnotation == null && controllerAnnotation == null) {
            throw new JoltDIException(
                    "Class " + beanClass.getName() + " is not annotated with @JoltBean or @Controller");
        }
        String beanName = getBeanName(beanClass, joltBeanAnnotation);
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
            Bean annotation = beanClass.getAnnotation(Bean.class);
            if (annotation != null && annotation.initialization() == InitializationMode.EAGER &&
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
    @Deprecated(since = "1.0", forRemoval = false)
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
     * Retrieves all beans of a given parent type.
     * 
     * @param <T>        The expected bean type.
     * @param parentType The parent type.
     * @return A list of beans of the given parent type.
     * @throws BeanNotFoundException if no beans are found assignable to the given
     *                               type.
     */
    public <T> List<T> getBeans(Class<T> parentType) {
        Objects.requireNonNull(parentType, "Parent type cannot be null");

        List<T> beans = new ArrayList<>();
        for (Map.Entry<String, Class<?>> entry : beanDefinitions.entrySet()) {
            if (parentType.isAssignableFrom(entry.getValue())) {
                beans.add(parentType.cast(getBean(entry.getKey())));
            }
        }

        if (beans.isEmpty()) {
            throw new BeanNotFoundException("No beans found of parent type: " + parentType.getName());
        }
        return beans;
    }

    /**
     * Retrieves all beans annotated with the specified annotation.
     * 
     * @param annotation The annotation to filter beans by.
     * @param <T>        The expected bean type.
     * @return A list of beans annotated with the specified annotation.
     * @throws NullPointerException if the annotation is null.
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getBeans(Annotation annotation) {
        Objects.requireNonNull(annotation, "Annotation cannot be null");
        Class<? extends Annotation> annotationType = annotation.annotationType();
        List<T> beans = new ArrayList<>();

        for (Map.Entry<String, Class<?>> entry : beanDefinitions.entrySet()) {
            if (entry.getValue().isAnnotationPresent(annotationType)) {
                beans.add((T) getBean(entry.getKey()));
            }
        }

        if (beans.isEmpty()) {
            logger.warning("No beans found with annotation: " + annotationType.getName());
        }
        return beans;
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

        for (Map.Entry<String, Class<?>> entry : beanDefinitions.entrySet()) {
            if (type.isAssignableFrom(entry.getValue()) && entry.getKey().equals(name)) {
                logger.info("Creating bean instance for type: " + type.getName() +
                        " using bean name: " + entry.getKey());
                return type.cast(createBean(entry.getValue(), entry.getKey()));
            }
        }
        throw new BeanNotFoundException("No bean found of type: " + type.getName() + " and name: " + name);
    }

    /**
     * Injects dependencies into the given object instance.
     * This can be used for both managed beans and external objects.
     *
     * @param instance the object instance to inject dependencies into
     */
    public void inject(Object instance) {
        Objects.requireNonNull(instance, "Instance cannot be null");
        Class<?> clazz = instance.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowire.class)) {
                try {
                    field.setAccessible(true);
                    Autowire injection = field.getAnnotation(Autowire.class);
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
            Bean annotation = beanClass.getAnnotation(Bean.class);
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
            if (field.isAnnotationPresent(Autowire.class)) {
                try {
                    field.setAccessible(true);
                    var injection = field.getAnnotation(Autowire.class);
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

    private String getBeanName(Class<?> beanClass, Bean annotation) {
        if (annotation != null && !annotation.value().isEmpty()) {
            return annotation.value();
        }
        return Character.toLowerCase(beanClass.getSimpleName().charAt(0))
                + beanClass.getSimpleName().substring(1);
    }
}