package ca.jolt.core;

import java.util.function.Supplier;
import java.util.logging.Logger;

import ca.jolt.exceptions.ServerException;
import ca.jolt.injector.JoltContainer;
import ca.jolt.logging.LogConfigurator;
import ca.jolt.logging.StartupLog;
import ca.jolt.routing.RouteHandler;
import ca.jolt.server.TomcatServer;
import ca.jolt.server.config.ConfigurationManager;
import ca.jolt.server.config.ServerConfig;

/**
 * An abstract base class for a Jolt application that sets up logging,
 * initializes
 * the IoC container, and manages the application’s embedded Tomcat server.
 * 
 * <p>
 * This class provides a simple DSL for creating routes (e.g., {@code get},
 * {@code post},
 * {@code put}, {@code delete}) and handles launching and stopping the server.
 * Subclasses
 * should implement {@link #setup()} to define the application's routes and any
 * additional configuration.
 * </p>
 * 
 * <p>
 * <strong>Usage Example:</strong>
 * 
 * <pre>
 * public class MyApp extends JoltApplication {
 * 
 *     public static void main(String[] args) {
 *         launch(MyApp.class);
 *     }
 * 
 *     &#64;Override
 *     protected void setup() {
 *         get("/", () -> "Hello, World!");
 *         get("/user/{id:int}", ctx -> "Hello, User #" + ctx.path("id"));
 *     }
 * }
 * </pre>
 * </p>
 * 
 * @author William Beaudin
 * @since 1.0
 */
public abstract class JoltApplication {

    private static final Logger log = Logger.getLogger(JoltApplication.class.getName());
    private static JoltApplication instance;

    /**
     * The {@link Router} used to map HTTP methods and paths to handling logic.
     */
    protected final Router router;

    /**
     * The embedded Tomcat server.
     */
    protected TomcatServer server;

    /**
     * Constructs a new {@code JoltApplication}. This constructor also performs:
     * <ul>
     * <li>A sanity check to ensure that only one instance exists in the JVM.</li>
     * <li>Startup logging initialization via {@link StartupLog}.</li>
     * <li>Logging configuration via {@link LogConfigurator}.</li>
     * <li>Scanning and initialization of beans within the {@code ca.jolt} package
     * using
     * {@link JoltContainer}.</li>
     * <li>Acquisition of a {@link Router} bean from {@link JoltContainer}.</li>
     * </ul>
     * 
     * @throws IllegalStateException
     *                               if an attempt is made to instantiate more than
     *                               one {@code JoltApplication}.
     */
    protected JoltApplication() {
        if (instance != null) {
            throw new IllegalStateException("Only one JoltApplication instance is allowed per JVM");
        }
        StartupLog.printStartup();
        LogConfigurator.configure();
        log.info("JoltApplication initialized");
        JoltContainer.getInstance().scan("ca.jolt").initialize();
        router = JoltContainer.getInstance().getBean(Router.class);
    }

    /**
     * Launches the Jolt application. This method reflectively instantiates the
     * specified
     * subclass of {@code JoltApplication}, calls its {@link #setup()} method to
     * configure
     * routes and server settings, and then starts the embedded Tomcat server.
     * 
     * <p>
     * If an exception occurs during launch, the application logs it at
     * {@code SEVERE}
     * level and exits the JVM with status code {@code 1}.
     * </p>
     *
     * @param <T>
     *                 A concrete subclass of {@code JoltApplication}.
     * @param appClass
     *                 The class object of the application subclass to launch.
     * @throws IllegalStateException
     *                               if an instance of the application is already
     *                               running.
     */
    public static <T extends JoltApplication> void launch(Class<T> appClass) {
        try {
            if (instance == null) {
                instance = appClass.getDeclaredConstructor().newInstance();
            }
            instance.setup();
            ServerConfig config = ConfigurationManager.getInstance().getServerConfig();
            instance.server = new TomcatServer(config);
            instance.server.start();
            log.info("Server started successfully!");
        } catch (Exception e) {
            log.severe("Failed to launch application: " + e.getMessage());

            StringBuilder stackTrace = new StringBuilder();
            stackTrace.append("Stack trace:\n");
            for (StackTraceElement element : e.getStackTrace()) {
                stackTrace.append("    at ").append(element.toString()).append("\n");
            }
            log.severe(stackTrace.toString());

            Throwable cause = e.getCause();
            if (cause != null) {
                log.severe("Caused by: " + cause.getMessage());
                StringBuilder causeStackTrace = new StringBuilder();
                for (StackTraceElement element : cause.getStackTrace()) {
                    causeStackTrace.append("    at ").append(element.toString()).append("\n");
                }
                log.severe(causeStackTrace.toString());
            }

            System.exit(1);
        }
    }

    /**
     * Stops the running Jolt application by shutting down the embedded Tomcat
     * server
     * gracefully. If the server is not running, this method does nothing.
     * 
     * <p>
     * Any {@link ServerException} thrown during shutdown is printed to the error
     * stream.
     * </p>
     */
    public static void stop() {
        if (instance != null && instance.server != null) {
            try {
                instance.server.stop();
            } catch (ServerException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * A lifecycle method that subclasses override to configure routes and server
     * settings for the application. This method is called automatically by
     * {@link #launch(Class)} after the application is instantiated.
     */
    protected abstract void setup();

    /**
     * Defines an HTTP GET route. The {@code handler} is used to process incoming
     * GET requests matching the specified path.
     *
     * @param path
     *                The path pattern (e.g., {@code "/user/{id:int}"}) to match.
     * @param handler
     *                A {@link RouteHandler} that processes the request and produces
     *                a response.
     */
    protected static void get(String path, RouteHandler handler) {
        instance.router.get(path, handler);
    }

    /**
     * Defines an HTTP GET route that produces a response via the provided
     * {@link Supplier}.
     * This variant is often useful for returning simple static responses without
     * needing
     * the request context.
     *
     * @param path
     *                 The path pattern to match.
     * @param supplier
     *                 A {@link Supplier} whose {@code get()} method returns the
     *                 response body.
     */
    protected static void get(String path, Supplier<Object> supplier) {
        instance.router.get(path, supplier);
    }

    /**
     * Defines an HTTP POST route. The {@code handler} is used to process incoming
     * POST requests matching the specified path.
     *
     * @param path
     *                The path pattern to match.
     * @param handler
     *                A {@link RouteHandler} that processes the request and produces
     *                a response.
     */
    protected static void post(String path, RouteHandler handler) {
        instance.router.post(path, handler);
    }

    /**
     * Defines an HTTP POST route that produces a response via the provided
     * {@link Supplier}.
     *
     * @param path
     *                 The path pattern to match.
     * @param supplier
     *                 A {@link Supplier} whose {@code get()} method returns the
     *                 response body.
     */
    protected static void post(String path, Supplier<Object> supplier) {
        instance.router.post(path, supplier);
    }

    /**
     * Defines an HTTP PUT route. The {@code handler} is used to process incoming
     * PUT requests matching the specified path.
     *
     * @param path
     *                The path pattern to match.
     * @param handler
     *                A {@link RouteHandler} that processes the request and produces
     *                a response.
     */
    protected static void put(String path, RouteHandler handler) {
        instance.router.put(path, handler);
    }

    /**
     * Defines an HTTP PUT route that produces a response via the provided
     * {@link Supplier}.
     *
     * @param path
     *                 The path pattern to match.
     * @param supplier
     *                 A {@link Supplier} whose {@code get()} method returns the
     *                 response body.
     */
    protected static void put(String path, Supplier<Object> supplier) {
        instance.router.put(path, supplier);
    }

    /**
     * Defines an HTTP DELETE route. The {@code handler} is used to process incoming
     * DELETE requests matching the specified path.
     *
     * @param path
     *                The path pattern to match.
     * @param handler
     *                A {@link RouteHandler} that processes the request and produces
     *                a response.
     */
    protected static void delete(String path, RouteHandler handler) {
        instance.router.delete(path, handler);
    }

    /**
     * Defines an HTTP DELETE route that produces a response via the provided
     * {@link Supplier}.
     *
     * @param path
     *                 The path pattern to match.
     * @param supplier
     *                 A {@link Supplier} whose {@code get()} method returns the
     *                 response body.
     */
    protected static void delete(String path, Supplier<Object> supplier) {
        instance.router.delete(path, supplier);
    }
}