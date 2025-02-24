package ca.jolt.injector;

import java.util.logging.Logger;

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

    private final BeanRegistry beanRegistry = new BeanRegistry();
    private final ConfigurationManager configurationManager = new ConfigurationManager();
    private final BeanScanner beanScanner = new BeanScanner(beanRegistry, configurationManager);
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
     * </p>
     *
     * <p>
     * <strong>Note:</strong> This method can only be called once. After
     * initialization, no further
     * modifications to the bean definitions are permitted.
     * </p>
     */
    public void initialize() {
        if (!isInitialized) {
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
        return beanRegistry.getBean(type);
    }
}