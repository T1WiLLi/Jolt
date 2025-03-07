package ca.jolt.injector;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import ca.jolt.exceptions.BeanCreationException;
import ca.jolt.exceptions.BeanNotFoundException;
import ca.jolt.exceptions.CircularDependencyException;
import ca.jolt.exceptions.JoltDIException;
import ca.jolt.injector.annotation.JoltBean;
import ca.jolt.injector.annotation.JoltBeanInjection;
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
 *
 * <p>
 * <strong>Dependency Injection and Lifecycle Management:</strong><br>
 * When a bean is created, its fields annotated with {@link JoltBeanInjection}
 * are automatically populated.
 * The container also ensures that lifecycle methods annotated with
 * {@link jakarta.annotation.PostConstruct} and
 * {@link jakarta.annotation.PreDestroy}
 * are invoked at the appropriate times. For singleton beans, an instance is
 * maintained and reused upon subsequent requests.
 *
 * <p>
 * <strong>Exception Handling and Circular Dependencies:</strong><br>
 * The container checks for potential circular dependencies during bean creation
 * and throws a
 * {@link CircularDependencyException} if one is detected. If a bean cannot be
 * found or fails to create,
 * {@link BeanNotFoundException} or {@link BeanCreationException} is thrown,
 * respectively.
 *
 * <p>
 * <strong>Usage Example:</strong>
 * <blockquote>
 * 
 * <pre>
 * JoltContainer.getInstance()
 *         .scan("com.example.myapp.beans")
 *         .initialize();
 *
 * MyBean bean = container.getBean(MyBean.class);
 * // Use the bean...
 *
 * JoltContainer.getInstance().shutdown();
 * </pre>
 * 
 * </blockquote>
 *
 * <p>
 * <em>Note:</em> This container is designed to be simple and lightweight. It
 * may not include some features
 * found in more comprehensive DI frameworks.
 *
 * @author William Beaudin
 * @since 1.0
 */
public final class JoltContainer {

    private static Logger logger = Logger.getLogger(JoltContainer.class.getName());

    @Getter
    private static final JoltContainer instance = new JoltContainer();

    private final BeanRegistry beanRegistry = new BeanRegistry();
    private final ConfigurationManager configurationManager = new ConfigurationManager();
    private final BeanScanner beanScanner = new BeanScanner(beanRegistry, configurationManager);
    private boolean isInitialized = false;

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
     *
     * @param directory the base package to scan for bean classes; must not be
     *                  {@code null} or empty.
     * @throws JoltDIException if the base package is {@code null}, empty, not
     *                         found, or if an error occurs during scanning.
     * @return the container instance for method chaining.
     */
    public JoltContainer scan(String directory) throws JoltDIException {
        beanScanner.scanPackage(directory);
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
     *
     * <p>
     * <strong>Note:</strong> This method can only be called once. After
     * initialization, no further
     * modifications to the bean definitions are permitted.
     */
    public void initialize() {
        if (!isInitialized) {
            configurationManager.initializeConfigurations();
            beanRegistry.initializeEagerBeans();
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
     * annotated with {@link jakarta.annotation.PreDestroy}. After invoking these
     * callbacks, all
     * internal caches, bean definitions,
     * and tracking collections are cleared, effectively resetting the container.
     *
     * <p>
     * This method should be called during application shutdown to ensure that
     * resources are properly released
     * and any cleanup logic is executed.
     */
    public void shutdown() {
        logger.info("Shutting down container. Invoking @PreDestroy methods.");
        beanRegistry.shutdown();
        configurationManager.clear();
        isInitialized = false;
        logger.info("Container shutdown complete.");
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
    @Deprecated(since = "1.0", forRemoval = false)
    public <T> T getBean(String name) {
        return beanRegistry.getBean(name);
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
     *
     * <p>
     * <strong>Type Safety:</strong> The returned object is cast using
     * {@code type.cast(...)} ensuring that the
     * runtime type is checked against the provided class.
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
        try {
            return beanRegistry.getBean(type);
        } catch (BeanNotFoundException e) {
            try {
                return configurationManager.getConfiguration(type);
            } catch (JoltDIException e2) {
                throw new BeanNotFoundException("No bean or configuration bean found for type: " + type.getName());
            }
        }
    }

    /**
     * Retrieves a list of beans that are instances of the specified parent type.
     *
     * <p>
     * This method searches through all registered bean definitions and returns a
     * list
     * of beans whose classes are assignable to the specified parent type. If no
     * beans
     * are found, an empty list is returned.
     *
     * <p>
     * <strong>Type Safety:</strong> The returned objects are cast using
     * {@code parentType.cast(...)} ensuring that the runtime type is checked
     * against
     * the provided class.
     *
     * @param <T>        the type of the beans to be retrieved
     * @param parentType the class object representing the parent type of the beans;
     *                   must not be {@code null}.
     * @return a list of beans that are instances of the specified parent type.
     * @throws NullPointerException if the {@code parentType} is {@code null}.
     */
    public <T> List<T> getBeans(Class<T> parentType) {
        return beanRegistry.getBeans(parentType);
    }

    /**
     * Retrieves a bean instance by its type and name.
     * 
     * <p>
     * This method searches through all registered bean definitions and names to
     * returns the
     * first beans to match the specified type and name.
     * If the bean has already been instantiate (for example, as a singleton)
     * the cached instance is returned. Otherwise, the bean is instantiated, its
     * dependencies injected and its lifecycle callbacks invoked.
     * 
     * @param <T>  the expected type of the bean.
     * @param type the class type of the bean to retrieve; must not be {@code null}.
     * @param name the name of the class as {@code value} set in the annotation.
     * @return an instance of the bean matching the specfified type and name.
     * 
     * @throws BeanNotFoundException if no bean is found that is
     *                               assignable to the
     *                               specified type.
     * @throws BeanCreationException if an error occurs during the creation or
     *                               initialization of the bean.
     */
    public <T> T getBean(Class<T> type, String name) {
        return beanRegistry.getBean(type, name);
    }

    private JoltContainer() {
        // Private constructor to prevent instantiation
    }
}