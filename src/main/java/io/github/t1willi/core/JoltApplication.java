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
    public static void before(Consumer<JoltContext> handler, String... routes) {
        router.before(handler, routes);
    }

    public static void after(Consumer<JoltContext> handler, String... routes) {
        router.after(handler, routes);
    }

    protected static void get(String path, RouteHandler handler) {
        router.get(path, handler);
    }

    protected static void post(String path, RouteHandler handler) {
        router.post(path, handler);
    }

    protected static void put(String path, RouteHandler handler) {
        router.put(path, handler);
    }

    protected static void delete(String path, RouteHandler handler) {
        router.delete(path, handler);
    }

    protected static void route(HttpMethod method, String path, RouteHandler handler) {
        router.route(method, path, handler);
    }

    protected static void group(String base, Runnable block) {
        router.group(base, block);
    }

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