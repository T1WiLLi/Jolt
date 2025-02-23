package ca.jolt.injector;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import ca.jolt.exceptions.BeanCreationException;
import ca.jolt.exceptions.BeanNotFoundException;
import ca.jolt.exceptions.CircularDependencyException;
import ca.jolt.exceptions.JoltDIException;
import ca.jolt.injector.annotation.JoltBean;
import ca.jolt.injector.annotation.JoltBeanInjection;
import ca.jolt.injector.annotation.PostConstruct;
import ca.jolt.injector.annotation.PreDestroy;
import lombok.Getter;

/**
 * The {@code JoltContainer} class serves as a lightweight dependency injection
 * container.
 * It is responsible for scanning specified packages to discover bean classes,
 * registering them,
 * managing their instantiation, performing dependency injection, and handling
 * lifecycle callbacks.
 *
 * <p>
 * <strong>Bean Discovery and Registration:</strong><br>
 * The container scans a given base package for classes annotated with
 * {@link JoltBean}. Once discovered,
 * these classes are validated (ensuring they are concrete and have a public
 * no-argument constructor)
 * and then registered. Each bean is identified by a unique name derived either
 * from the {@code value}
 * attribute of the {@code JoltBean} annotation or, if empty, by a convention
 * that lowercases the first
 * letter of the class's simple name.
 * </p>
 *
 * <p>
 * <strong>Dependency Injection and Lifecycle Management:</strong><br>
 * When a bean is created, its fields annotated with {@link JoltBeanInjection}
 * are automatically populated.
 * The container also ensures that lifecycle methods annotated with
 * {@link PostConstruct} and {@link PreDestroy}
 * are invoked at the appropriate times. For singleton beans, an instance is
 * maintained and reused upon subsequent requests.
 * </p>
 *
 * <p>
 * <strong>Exception Handling and Circular Dependencies:</strong><br>
 * The container checks for potential circular dependencies during bean creation
 * and throws a
 * {@link CircularDependencyException} if one is detected. If a bean cannot be
 * found or fails to create,
 * {@link BeanNotFoundException} or {@link BeanCreationException} is thrown,
 * respectively.
 * </p>
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <blockquote>
 * 
 * <pre>
 * JoltContainer container = new JoltContainer();
 * container.scanPackage("com.example.myapp.beans");
 * container.initialize();
 *
 * MyBean bean = container.getBean(MyBean.class);
 * // Use the bean...
 *
 * container.shutdown();
 * </pre>
 * 
 * </blockquote>
 * </p>
 *
 * <p>
 * <em>Note:</em> This container is designed to be simple and lightweight. It
 * may not include some features
 * found in more comprehensive DI frameworks, but it can be extended or modified
 * to fit specific needs.
 * </p>
 *
 * @author William Beaudin
 * @since 1.0
 */
public final class JoltContainer {

    private static Logger logger = Logger.getLogger(JoltContainer.class.getName());

    @Getter
    private static final JoltContainer instance = new JoltContainer();

    private final Map<String, Class<?>> beanDefinitions = new HashMap<>();
    private final Map<String, Object> singletonInstances = new ConcurrentHashMap<>();
    private final Map<Class<?>, Object> typeToInstance = new ConcurrentHashMap<>();
    private final Set<String> beansInCreation = Collections.synchronizedSet(new HashSet<>());
    private final List<Object> managedBeans = new ArrayList<>();
    private boolean isInitialized = false;

    private JoltContainer() {
        // Private constructor to prevent instantiation
    }

    /**
     * Scans the specified base package to discover and register all classes
     * annotated with {@link JoltBean}.
     *
     * <p>
     * This method converts the package name to a path, retrieves all resources for
     * that path,
     * and then recursively searches directories for class files. For each class
     * file found,
     * the class is loaded and, if it carries the {@code JoltBean} annotation, it is
     * validated and registered.
     * </p>
     *
     * @param basePackage the base package to scan for bean classes; must not be
     *                    {@code null} or empty.
     * @throws JoltDIException if the base package is {@code null}, empty, not
     *                         found, or if an error occurs during scanning.
     */
    public JoltContainer scanPackage(String basePackage) throws JoltDIException {
        logger.info("Scanning package: " + basePackage);
        validateState();
        Objects.requireNonNull(basePackage, "Base package cannot be null");

        if (basePackage.trim().isEmpty()) {
            throw new JoltDIException("Base package cannot be empty");
        }

        try {
            String path = basePackage.replace('.', '/');
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> resources = classLoader.getResources(path);

            if (!resources.hasMoreElements()) {
                throw new JoltDIException("Package not found: " + basePackage);
            }

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                File directory = new File(resource.getFile());
                findAndRegisterBeans(directory, basePackage);
            }
        } catch (Exception e) {
            logger.severe("Error scanning package: " + e.getMessage());
            throw new JoltDIException("Failed to scan package: " + basePackage, e);
        }

        return this;
    }

    /**
     * Initializes the container by eagerly instantiating all non-lazy beans that
     * have a singleton scope.
     *
     * <p>
     * Upon invocation, this method iterates over the registered bean definitions
     * and, for each bean
     * that is marked as non-lazy and scoped as a singleton, it triggers bean
     * creation. This ensures that
     * these beans are ready for use and that their {@code PostConstruct} lifecycle
     * methods are invoked.
     * </p>
     *
     * <p>
     * <strong>Note:</strong> This method can only be called once. After
     * initialization, no further
     * modifications to the bean definitions are permitted.
     * </p>
     */
    public void initialize() {
        logger.info("Initializing container with non-lazy singleton beans.");
        if (!isInitialized) {
            for (Map.Entry<String, Class<?>> entry : beanDefinitions.entrySet()) {
                Class<?> beanClass = entry.getValue();
                JoltBean annotation = beanClass.getAnnotation(JoltBean.class);
                if (!annotation.lazy() && "singleton".equals(annotation.scope())) {
                    logger.info("Eagerly creating bean instance for: " + entry.getKey());
                    getBean(entry.getKey());
                }
            }
            isInitialized = true;
            logger.info("Container initialization complete.");
        }
    }

    /**
     * Shuts down the container and releases all resources.
     *
     * <p>
     * During shutdown, the container iterates over all managed beans and invokes
     * any lifecycle methods
     * annotated with {@link PreDestroy}. After invoking these callbacks, all
     * internal caches, bean definitions,
     * and tracking collections are cleared, effectively resetting the container.
     * </p>
     *
     * <p>
     * This method should be called during application shutdown to ensure that
     * resources are properly released
     * and any cleanup logic is executed.
     * </p>
     */
    public void shutdown() {
        logger.info("Shutting down container. Invoking @PreDestroy methods.");
        for (Object bean : managedBeans) {
            try {
                invokeLifecycleMethod(bean, PreDestroy.class);
            } catch (Exception e) {
                logger.warning("Failed to invoke @PreDestroy method: " + bean.getClass().getName());
            }
        }
        managedBeans.clear();
        singletonInstances.clear();
        typeToInstance.clear();
        beanDefinitions.clear();
        beansInCreation.clear();
        isInitialized = false;
        logger.info("Container shutdown complete.");
    }

    public void registerBean(Class<?> beanClass) {
        logger.info("Registering bean: " + beanClass.getName());
        validateState();
        Objects.requireNonNull(beanClass, "Bean class cannot be null");

        JoltBean annotation = beanClass.getAnnotation(JoltBean.class);
        if (annotation != null) {
            String beanName = getBeanName(beanClass, annotation);

            if (beanDefinitions.containsKey(beanName)) {
                throw new JoltDIException("Duplicate bean name: " + beanName);
            }

            beanDefinitions.put(beanName, beanClass);

            if (!annotation.lazy()) {
                getBean(beanName);
            }
        }
    }

    /**
     * Retrieves a bean instance by its registered name.
     *
     * <p>
     * If the bean instance already exists (in case of singleton scope), it is
     * returned directly. Otherwise,
     * the bean is created, its dependencies are injected, and any
     * {@code PostConstruct} lifecycle callbacks are
     * invoked. The created bean is then stored in the singleton cache (if
     * applicable) and returned.
     * </p>
     *
     * @deprecated This method is not thread-safe and as been marked for removal.
     *             You should use
     *             {@link #getBean(Class)} instead.
     * 
     * @param name the unique name of the bean to retrieve; must not be
     *             {@code null}.
     * @param <T>  the expected type of the bean.
     * @return the bean instance corresponding to the given name.
     * @throws BeanNotFoundException if no bean is found with the specified name.
     * @throws BeanCreationException if there is an error during the creation or
     *                               initialization of the bean.
     */
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
     * Retrieves a bean instance by its type.
     *
     * <p>
     * This method searches through all registered bean definitions and returns the
     * first bean whose class is
     * assignable to the specified type. If the bean has already been created (for
     * example, if it is a singleton),
     * the cached instance is returned. Otherwise, the bean is instantiated, its
     * dependencies injected, and
     * its lifecycle callbacks invoked.
     * </p>
     *
     * <p>
     * <strong>Type Safety:</strong> The returned object is cast using
     * {@code type.cast(...)} ensuring that the
     * runtime type is checked against the provided class.
     * </p>
     *
     * @param type the class type of the bean to retrieve; must not be {@code null}.
     * @param <T>  the expected type of the bean.
     * @return an instance of the bean matching the specified type.
     * @throws BeanNotFoundException if no bean is found that is assignable to the
     *                               specified type.
     * @throws BeanCreationException if an error occurs during the creation or
     *                               initialization of the bean.
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
     * Recursively scans a directory for class files and registers any class that is
     * annotated with {@link JoltBean}.
     *
     * @param directory   the directory to scan.
     * @param basePackage the current package name corresponding to the directory.
     * @throws JoltDIException if the directory does not exist.
     */
    private void findAndRegisterBeans(File directory, String basePackage) {
        if (!directory.exists()) {
            throw new JoltDIException("Directory does not exist: " + directory.getPath());
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findAndRegisterBeans(file, basePackage + "." + file.getName());
                } else if (file.getName().endsWith(".class")) {
                    processClassFile(file, basePackage);
                }
            }
        }
    }

    /**
     * Loads a class file and, if annotated with {@link JoltBean}, validates and
     * registers it.
     *
     * @param file        the class file.
     * @param basePackage the package name corresponding to the file location.
     * @throws JoltDIException if the class cannot be loaded.
     */
    private void processClassFile(File file, String basePackage) {
        String className = basePackage + "." +
                file.getName().substring(0, file.getName().length() - 6);
        try {
            Class<?> clazz = Class.forName(className);
            if (clazz.isAnnotationPresent(JoltBean.class)) {
                validateBeanClass(clazz);
                registerBean(clazz);
            }
        } catch (ClassNotFoundException e) {
            throw new JoltDIException("Failed to load class: " + className, e);
        }
    }

    /**
     * Validates that the given bean class meets the necessary criteria.
     *
     * <p>
     * The validation checks that the class is concrete (not abstract or an
     * interface), provides a public no-argument
     * constructor, and has valid lifecycle methods (only one {@code PostConstruct}
     * and one {@code PreDestroy} method,
     * each with no parameters).
     * </p>
     *
     * @param beanClass the bean class to validate.
     * @throws JoltDIException if the class is abstract, an interface, or does not
     *                         meet the constructor/lifecycle method requirements.
     */
    private void validateBeanClass(Class<?> beanClass) {
        if (Modifier.isAbstract(beanClass.getModifiers()) ||
                Modifier.isInterface(beanClass.getModifiers())) {
            throw new JoltDIException(
                    "Bean class must be concrete: " + beanClass.getName());
        }

        try {
            beanClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new JoltDIException(
                    "Bean class must have a no-arg constructor: " + beanClass.getName());
        }

        validateLifecycleMethods(beanClass, PostConstruct.class);
        validateLifecycleMethods(beanClass, PreDestroy.class);
    }

    /**
     * Validates that the bean class does not have multiple or improperly defined
     * lifecycle methods.
     *
     * <p>
     * Specifically, it checks that for the given annotation (either
     * {@code PostConstruct} or {@code PreDestroy}),
     * there is at most one method annotated and that this method takes no
     * parameters.
     * </p>
     *
     * @param beanClass      the class to validate.
     * @param annotationType the lifecycle annotation type to check for.
     * @throws JoltDIException if more than one method is annotated or if a
     *                         lifecycle method has parameters.
     */
    private void validateLifecycleMethods(Class<?> beanClass,
            Class<? extends Annotation> annotationType) {
        List<Method> annotatedMethods = Arrays.stream(beanClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(annotationType))
                .collect(Collectors.toList());

        if (annotatedMethods.size() > 1) {
            throw new JoltDIException(
                    "Multiple " + annotationType.getSimpleName() +
                            " methods found in " + beanClass.getName());
        }

        for (Method method : annotatedMethods) {
            if (method.getParameterCount() > 0) {
                throw new JoltDIException(
                        "Lifecycle method must have no parameters: " +
                                beanClass.getName() + "." + method.getName());
            }
        }
    }

    /**
     * Creates a new bean instance, performs dependency injection, and invokes its
     * {@code PostConstruct} lifecycle method.
     *
     * <p>
     * This method also checks for circular dependencies by tracking beans that are
     * in the process of creation.
     * If a circular dependency is detected, a {@link CircularDependencyException}
     * is thrown.
     * </p>
     *
     * @param beanClass the class of the bean to create.
     * @param beanName  the unique name of the bean.
     * @return the fully initialized bean instance.
     * @throws BeanCreationException       if instantiation fails, dependencies
     *                                     cannot be injected, or lifecycle methods
     *                                     throw errors.
     * @throws CircularDependencyException if a circular dependency is detected.
     */
    private Object createBean(Class<?> beanClass, String beanName) {
        if (!beansInCreation.add(beanName)) {
            throw new CircularDependencyException(
                    "Circular dependency detected for bean: " + beanName +
                            ". Creation chain: " + String.join(" -> ", beansInCreation));
        }

        try {
            Object instance = beanClass.getDeclaredConstructor().newInstance();

            JoltBean annotation = beanClass.getAnnotation(JoltBean.class);
            if ("singleton".equals(annotation.scope())) {
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
            logger.severe("Error creating bean " + beanName + ": " + e.getMessage());
            throw new BeanCreationException(
                    "Failed to create bean: " + beanName, e);
        }
    }

    /**
     * Injects dependencies into the given bean instance by setting fields annotated
     * with {@link JoltBeanInjection}.
     *
     * <p>
     * For each field that is marked with the injection annotation, the container
     * determines the required dependency
     * either by the specified bean name or by the field type. If the dependency is
     * not found and the injection is marked
     * as required, a {@link BeanNotFoundException} is thrown.
     * </p>
     *
     * @param instance the bean instance into which dependencies should be injected.
     * @throws BeanCreationException if a dependency cannot be injected due to
     *                               access restrictions or missing dependencies.
     */
    private void injectDependencies(Object instance) {
        Class<?> clazz = instance.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            JoltBeanInjection injection = field.getAnnotation(JoltBeanInjection.class);
            if (injection != null) {
                try {
                    field.setAccessible(true);
                    Object dependency = !injection.value().isEmpty() ? getBean(injection.value())
                            : getBean(field.getType());

                    if (dependency == null && injection.required()) {
                        throw new BeanNotFoundException(
                                "Required dependency not found for: " +
                                        clazz.getName() + "." + field.getName());
                    }

                    field.set(instance, dependency);
                } catch (IllegalAccessException e) {
                    logger.severe("Error injecting dependency for field: " + clazz.getName() + "." + field.getName());
                    throw new BeanCreationException(
                            "Failed to inject dependency: " +
                                    clazz.getName() + "." + field.getName(),
                            e);
                }
            }
        }
    }

    /**
     * Invokes a lifecycle method annotated with the specified annotation on the
     * given bean instance.
     *
     * <p>
     * This method is used to invoke methods annotated with {@link PostConstruct} or
     * {@link PreDestroy}.
     * If an error occurs during invocation, a {@link JoltDIException} is thrown.
     * </p>
     *
     * @param instance       the bean instance on which to invoke the lifecycle
     *                       method.
     * @param annotationType the lifecycle annotation to look for (e.g.,
     *                       {@code PostConstruct} or {@code PreDestroy}).
     * @throws JoltDIException if the lifecycle method throws an exception during
     *                         invocation.
     */
    private void invokeLifecycleMethod(Object instance, Class<? extends Annotation> annotationType) {
        Class<?> clazz = instance.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotationType)) {
                try {
                    method.setAccessible(true);
                    method.invoke(instance);
                } catch (Exception e) {
                    throw new JoltDIException(
                            "Failed to invoke lifecycle method on: " +
                                    instance.getClass().getName(),
                            e);
                }
            }
        }
    }

    /**
     * Determines the unique name of a bean based on its {@link JoltBean}
     * annotation.
     *
     * <p>
     * If the {@code value} attribute of the annotation is empty, the bean name is
     * derived by lowercasing
     * the first character of the class's simple name. Otherwise, the provided value
     * is used as the bean name.
     * </p>
     *
     * @param beanClass  the class of the bean.
     * @param annotation the {@code JoltBean} annotation present on the bean class.
     * @return the unique name for the bean.
     */
    private String getBeanName(Class<?> beanClass, JoltBean annotation) {
        String value = annotation.value();
        return value.isEmpty() ? Character.toLowerCase(beanClass.getSimpleName().charAt(0)) +
                beanClass.getSimpleName().substring(1) : value;
    }

    /**
     * Validates that the container is in a state that permits modifications to bean
     * definitions.
     *
     * <p>
     * Once the container has been initialized, further modifications (such as
     * registering additional beans)
     * are not allowed and will result in a {@link JoltDIException}.
     * </p>
     *
     * @throws JoltDIException if the container has already been initialized.
     */
    private void validateState() {
        if (isInitialized) {
            throw new JoltDIException(
                    "Container is already initialized. Cannot modify bean definitions.");
        }
    }
}