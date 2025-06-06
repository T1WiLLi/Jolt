package io.github.t1willi.core;

import java.util.function.Consumer;
import java.util.logging.Logger;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.database.Database;
import io.github.t1willi.exceptions.ServerException;
import io.github.t1willi.http.HttpMethod;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.logging.LogConfigurator;
import io.github.t1willi.logging.StartupLog;
import io.github.t1willi.openapi.annotations.OpenApi;
import io.github.t1willi.routing.RouteHandler;
import io.github.t1willi.server.TomcatServer;
import io.github.t1willi.server.config.ConfigurationManager;
import io.github.t1willi.server.config.ServerConfig;

/**
 * An abstract base class for a Jolt application that sets up logging and
 * initializes the IoC container.
 * <p>
 * This class manages the embedded Tomcat server, provides a DSL for creating
 * routes, and enforces a single running instance per JVM. Subclasses implement
 * {@link #init()} to define the application's routes and any additional
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
 *     protected void init() {
 *         get("/", ctx -> ctx.text("Hello, World!"));
 *         get("/user/{id}", ctx -> ctx.html("Hello, User #" + ctx.path("id")));
 *     }
 * }
 * }</pre>
 *
 * @author William
 * @since 1.0
 */
public abstract class JoltApplication {

    private static final Logger log = Logger.getLogger(JoltApplication.class.getName());
    private static JoltApplication instance;
    protected static Router router;
    protected TomcatServer server;

    /**
     * Constructs a new Jolt application.
     * Ensures that only one instance is allowed per JVM.
     *
     * @throws IllegalStateException if more than one instance is created
     */
    protected JoltApplication() {
        ensureSingleInstance();
        initializeLogging();
        log.info("JoltApplication initialized");
    }

    /**
     * Launches a Jolt application of the specified type.
     *
     * @param <T>      A concrete subclass of {@code JoltApplication}
     * @param appClass The class of the application subclass to launch
     * @throws IllegalStateException if an instance is already running
     */
    public static <T extends JoltApplication> void launch(Class<T> appClass) {
        try {
            createApplicationInstance(appClass);
            initializeApplication();
            startServer();
            log.info("Server started successfully!");
        } catch (Exception e) {
            handleLaunchFailure(e);
        }
    }

    /**
     * Stops the running Jolt application, if any.
     */
    public static void stop() {
        if (isServerRunning()) {
            stopServer();
        }
    }

    /**
     * Returns the OpenApi annotation if present on the application class.
     */
    public static OpenApi openApi() {
        return getOpenApiAnnotation();
    }

    /**
     * A lifecycle method that subclasses override to configure routes and server
     * settings.
     */
    protected void init() {
        // No-op by default.
    }

    // Route registration methods

    /**
     * Registers a "before" filter that executes before matching routes.
     * <p>
     * The specified handler will be invoked before any route that matches the given
     * route patterns.
     * This is typically used for tasks such as authentication, logging, or
     * modifying the request context
     * before the main route handler is executed.
     *
     * @param handler the {@link Consumer} that accepts a {@link JoltContext} and
     *                performs pre-processing
     * @param routes  one or more route patterns (e.g., "/api/*") to which the
     *                filter applies; if empty, applies to all routes
     * @since 1.0
     */
    public static void before(Consumer<JoltContext> handler, String... routes) {
        router.before(handler, routes);
    }

    /**
     * Registers an "after" filter that executes after matching routes.
     * <p>
     * The specified handler will be invoked after any route that matches the given
     * route patterns.
     * This is typically used for tasks such as response modification, logging, or
     * cleanup after the main
     * route handler has executed.
     *
     * @param handler the {@link Consumer} that accepts a {@link JoltContext} and
     *                performs post-processing
     * @param routes  one or more route patterns (e.g., "/api/*") to which the
     *                filter applies; if empty, applies to all routes
     * @since 1.0
     */
    public static void after(Consumer<JoltContext> handler, String... routes) {
        router.after(handler, routes);
    }

    /**
     * Registers a GET route handler for the specified path.
     * <p>
     * The handler will be invoked when an HTTP GET request matches the given path.
     *
     * @param path    the route path (e.g., "/users/{id}")
     * @param handler the {@link RouteHandler} to handle the request
     * @since 1.0
     */
    protected static void get(String path, RouteHandler handler) {
        router.get(path, handler);
    }

    /**
     * Registers a POST route handler for the specified path.
     * <p>
     * The handler will be invoked when an HTTP POST request matches the given path.
     *
     * @param path    the route path (e.g., "/users")
     * @param handler the {@link RouteHandler} to handle the request
     * @since 1.0
     */
    protected static void post(String path, RouteHandler handler) {
        router.post(path, handler);
    }

    /**
     * Registers a PUT route handler for the specified path.
     * <p>
     * The handler will be invoked when an HTTP PUT request matches the given path.
     *
     * @param path    the route path (e.g., "/users/{id}")
     * @param handler the {@link RouteHandler} to handle the request
     * @since 1.0
     */
    protected static void put(String path, RouteHandler handler) {
        router.put(path, handler);
    }

    /**
     * Registers a DELETE route handler for the specified path.
     * <p>
     * The handler will be invoked when an HTTP DELETE request matches the given
     * path.
     *
     * @param path    the route path (e.g., "/users/{id}")
     * @param handler the {@link RouteHandler} to handle the request
     * @since 1.0
     */
    protected static void delete(String path, RouteHandler handler) {
        router.delete(path, handler);
    }

    /**
     * Registers a route handler for a custom HTTP method and path.
     * <p>
     * This method allows you to register handlers for any HTTP method (e.g., PATCH,
     * OPTIONS)
     * by specifying the {@link HttpMethod} explicitly.
     *
     * @param method  the HTTP method (e.g., {@link HttpMethod#PATCH})
     * @param path    the route path (e.g., "/users/{id}")
     * @param handler the {@link RouteHandler} to handle the request
     * @since 1.0
     */
    protected static void route(HttpMethod method, String path, RouteHandler handler) {
        router.route(method, path, handler);
    }

    /**
     * Groups multiple route definitions under a common base path.
     * <p>
     * This method allows you to organize related routes under a shared base path.
     * All routes defined within the {@code block} will be prefixed with the
     * specified base.
     *
     * @param base  the base path for the group (e.g., "/api")
     * @param block a {@link Runnable} containing route definitions to group
     * @since 1.0
     */
    protected static void group(String base, Runnable block) {
        router.group(base, block);
    }

    /**
     * Groups multiple route definitions under a common base path and version.
     * <p>
     * This method allows you to organize related routes under a shared base path
     * and version number.
     * All routes defined within the {@code block} will be prefixed with the
     * specified base and version.
     * For example, {@code group("/api", 2, () -> { ... })} will prefix routes with
     * "/api/v2".
     *
     * @param base    the base path for the group (e.g., "/api")
     * @param version the version number to append to the base path (e.g., 2 for
     *                "/api/v2")
     * @param block   a {@link Runnable} containing route definitions to group
     * @since 1.0
     */
    protected static void group(String base, int version, Runnable block) {
        router.group(base, version, block);
    }

    // Private helper methods to reduce complexity

    private void ensureSingleInstance() {
        if (instance != null) {
            throw new IllegalStateException("Only one JoltApplication instance is allowed per JVM");
        }
    }

    private void initializeLogging() {
        StartupLog.printStartup();
        LogConfigurator.configure();
    }

    private static <T extends JoltApplication> void createApplicationInstance(Class<T> appClass) throws Exception {
        if (instance == null) {
            instance = appClass.getDeclaredConstructor().newInstance();
        }
    }

    private static void initializeApplication() throws Exception {
        initializeContainer();
        initializeDatabase();
        initializeRouter();
        registerControllers();
        instance.init();
    }

    private static void initializeContainer() throws Exception {
        JoltContainer container = JoltContainer.getInstance();
        container.autoScan();
        container.initialize();
    }

    private static void initializeDatabase() {
        Database.init();
    }

    private static void initializeRouter() {
        router = JoltContainer.getInstance().getBean(Router.class);
    }

    private static void registerControllers() {
        ControllerRegistry.registerControllers();
    }

    private static void startServer() throws Exception {
        ServerConfig config = ConfigurationManager.getInstance().getServerConfig();
        instance.server = new TomcatServer(config);
        instance.server.start();
    }

    private static void handleLaunchFailure(Exception e) {
        logError("Failed to launch application: " + e.getMessage());
        logStackTrace(e);
        logCauseIfPresent(e);
        System.exit(1);
    }

    private static void logError(String message) {
        log.severe(message);
    }

    private static void logStackTrace(Exception e) {
        StringBuilder stackTrace = buildStackTrace(e.getStackTrace());
        log.severe("Stack trace:\n" + stackTrace.toString());
    }

    private static void logCauseIfPresent(Exception e) {
        Throwable cause = e.getCause();
        if (cause != null) {
            log.severe("Caused by: " + cause.getMessage());
            StringBuilder causeStackTrace = buildStackTrace(cause.getStackTrace());
            log.severe(causeStackTrace.toString());
        }
    }

    private static StringBuilder buildStackTrace(StackTraceElement[] elements) {
        StringBuilder stackTrace = new StringBuilder();
        for (StackTraceElement element : elements) {
            stackTrace.append("    at ").append(element.toString()).append("\n");
        }
        return stackTrace;
    }

    private static boolean isServerRunning() {
        return instance != null && instance.server != null;
    }

    private static void stopServer() {
        try {
            instance.server.stop();
        } catch (ServerException e) {
            e.printStackTrace();
        }
    }

    private static OpenApi getOpenApiAnnotation() {
        Class<?> instanceClass = instance.getClass();
        return instanceClass.isAnnotationPresent(OpenApi.class)
                ? instanceClass.getAnnotation(OpenApi.class)
                : null;
    }
}