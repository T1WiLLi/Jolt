package ca.jolt.core;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import ca.jolt.exceptions.ServerException;
import ca.jolt.injector.JoltContainer;
import ca.jolt.logging.LogConfigurator;
import ca.jolt.logging.StartupLog;
import ca.jolt.routing.RouteHandler;
import ca.jolt.routing.context.JoltHttpContext;
import ca.jolt.server.TomcatServer;
import ca.jolt.server.config.ConfigurationManager;
import ca.jolt.server.config.ServerConfig;

/**
 * An abstract base class for a Jolt application that sets up logging and
 * initializes the IoC container.
 * <p>
 * This class manages the embedded Tomcat server, provides a DSL for creating
 * routes, and enforces a single running instance per JVM. Subclasses implement
 * {@link #setup()} to define the application's routes and any additional
 * configuration.
 * <p>
 * <strong>Usage Example:</strong>
 * 
 * <pre>{@code
 * public class MyApp extends JoltApplication {
 *
 *     public static void main(String[] args) {
 *         launch(MyApp.class);
 *     }
 *
 *     @Override
 *     protected void setup() {
 *         get("/", () -> "Hello, World!");
 *         get("/user/{id:int}", ctx -> "Hello, User #" + ctx.path("id"));
 *     }
 * }
 * }</pre>
 *
 * @author William
 * @since 1.0
 */
public abstract class JoltApplication {

    /**
     * The logger for this application.
     */
    private static final Logger log = Logger.getLogger(JoltApplication.class.getName());

    /**
     * A single global instance of this application.
     */
    private static JoltApplication instance;

    /**
     * A router for mapping HTTP methods and paths to handler logic.
     */
    protected static Router router;

    /**
     * The embedded Tomcat server.
     */
    protected TomcatServer server;

    /**
     * Constructs a new Jolt application.
     * <p>
     * Ensures that only one instance is allowed per JVM, and performs startup
     * logging and bean scanning.
     *
     * @throws IllegalStateException if more than one instance is created
     */
    protected JoltApplication() {
        if (instance != null) {
            throw new IllegalStateException("Only one JoltApplication instance is allowed per JVM");
        }
        StartupLog.printStartup();
        LogConfigurator.configure();
        log.info("JoltApplication initialized");
    }

    /**
     * Launches a Jolt application of the specified type.
     * <p>
     * Reflectively creates an instance of the given subclass, initializes its IoC
     * container, acquires a router, invokes the subclass's {@link #setup()} method,
     * and starts an embedded Tomcat server.
     * <p>
     * Logs any exceptions at SEVERE level and exits the JVM with status code 1
     * if a fatal error occurs during launch.
     *
     * @param <T>      A concrete subclass of {@code JoltApplication}
     * @param appClass The class of the application subclass to launch
     * @param scan     A package to scan for beans in addition to {@code ca.jolt}
     * @throws IllegalStateException if an instance is already running
     */
    public static <T extends JoltApplication> void launch(Class<T> appClass, String scan) {
        try {
            if (instance == null) {
                instance = appClass.getDeclaredConstructor().newInstance();
            }
            JoltContainer.getInstance().scan("ca.jolt");
            if (!scan.equals("ca.jolt") && !scan.isEmpty()) {
                JoltContainer.getInstance().scan(scan);
            }
            JoltContainer.getInstance().initialize();
            router = JoltContainer.getInstance().getBean(Router.class);
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
     * Stops the running Jolt application, if any.
     * <p>
     * Shuts down the embedded Tomcat server gracefully and prints any
     * {@link ServerException} to the error stream.
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
     * settings.
     * <p>
     * This method is invoked automatically by {@link #launch(Class, String)}.
     */
    protected abstract void setup();

    /**
     * Registers a global before-handler for all routes.
     *
     * @param handler A handler that executes before every route. Receives a
     *                {@link JoltHttpContext} for the current request.
     */
    public static void before(Consumer<JoltHttpContext> handler) {
        router.before(handler);
    }

    /**
     * Registers a before-handler for the specified routes.
     *
     * @param handler A handler that executes before each matching route.
     *                Receives a {@link JoltHttpContext} for the current request.
     * @param routes  One or more path patterns (e.g., "/doc", "/api").
     */
    public static void before(Consumer<JoltHttpContext> handler, String... routes) {
        router.before(handler, routes);
    }

    /**
     * Registers a global after-handler for all routes.
     *
     * @param handler A handler that executes after every route. Receives a
     *                {@link JoltHttpContext} for the current request.
     */
    public static void after(Consumer<JoltHttpContext> handler) {
        router.after(handler);
    }

    /**
     * Registers an after-handler for the specified routes.
     *
     * @param handler A handler that executes after each matching route.
     *                Receives a {@link JoltHttpContext} for the current request.
     * @param routes  One or more path patterns (e.g., "/doc", "/api").
     */
    public static void after(Consumer<JoltHttpContext> handler, String... routes) {
        router.after(handler, routes);
    }

    /**
     * Defines an HTTP GET route with a specified handler.
     *
     * @param path    The path pattern, for example "/user/{id:int}"
     * @param handler A {@link RouteHandler} that processes the request
     */
    protected static void get(String path, RouteHandler handler) {
        router.get(path, handler);
    }

    /**
     * Defines an HTTP GET route that uses a {@link Supplier} to produce responses.
     *
     * @param path     The path pattern to match
     * @param supplier A supplier whose {@code get()} method returns the response
     */
    protected static void get(String path, Supplier<Object> supplier) {
        router.get(path, supplier);
    }

    /**
     * Defines an HTTP POST route with a specified handler.
     *
     * @param path    The path pattern to match
     * @param handler A {@link RouteHandler} that processes the request
     */
    protected static void post(String path, RouteHandler handler) {
        router.post(path, handler);
    }

    /**
     * Defines an HTTP POST route that uses a {@link Supplier} to produce responses.
     *
     * @param path     The path pattern to match
     * @param supplier A supplier whose {@code get()} method returns the response
     */
    protected static void post(String path, Supplier<Object> supplier) {
        router.post(path, supplier);
    }

    /**
     * Defines an HTTP PUT route with a specified handler.
     *
     * @param path    The path pattern to match
     * @param handler A {@link RouteHandler} that processes the request
     */
    protected static void put(String path, RouteHandler handler) {
        router.put(path, handler);
    }

    /**
     * Defines an HTTP PUT route that uses a {@link Supplier} to produce responses.
     *
     * @param path     The path pattern to match
     * @param supplier A supplier whose {@code get()} method returns the response
     */
    protected static void put(String path, Supplier<Object> supplier) {
        router.put(path, supplier);
    }

    /**
     * Defines an HTTP DELETE route with a specified handler.
     *
     * @param path    The path pattern to match
     * @param handler A {@link RouteHandler} that processes the request
     */
    protected static void delete(String path, RouteHandler handler) {
        router.delete(path, handler);
    }

    /**
     * Defines an HTTP DELETE route that uses a {@link Supplier} to produce
     * responses.
     *
     * @param path     The path pattern to match
     * @param supplier A supplier whose {@code get()} method returns the response
     */
    protected static void delete(String path, Supplier<Object> supplier) {
        router.delete(path, supplier);
    }

    /**
     * Defines a group of routes sharing a common prefix path.
     *
     * @param base  The base path for the group of routes
     * @param block A {@link Runnable} that defines the nested routes
     */
    protected static void group(String base, Runnable block) {
        router.group(base, block);
    }
}