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
 * Base class for a Jolt application.
 * 
 * <p>
 * Usage Example:
 * 
 * <pre>
 * public class MyApp extends JoltApplication {
 *     public static void main(String[] args) {
 *         launch(MyApp.class, args);
 *     }
 * 
 *     {@literal @}Override
 *     protected void setup() {
 *         buildServer().withPort(8080);
 *         get("/", () -> "Hello, World!");
 *         // other route definitions...
 *     };
 * 
 *     <br>
 *     &nbsp;{@literal @}Override
 *     protected void configureRouting(WebServer server, Router router) {
 *         server.setRouter(router);
 *     }
 * }
 * </pre>
 * </p>
 */
public abstract class JoltApplication {

    private static final Logger log = Logger.getLogger(JoltApplication.class.getName());
    private static JoltApplication instance;

    protected final Router router;
    protected TomcatServer server;

    protected JoltApplication() {
        if (instance != null) {
            throw new IllegalStateException("Only one JoltApplication instance is allowed per JVM");
        }
        instance = this;
        StartupLog.printStartup();
        LogConfigurator.configure();
        log.info("JoltApplication initialized");
        JoltContainer.getInstance().scan("ca.jolt").initialize();
        router = JoltContainer.getInstance().getBean(Router.class);
    }

    /**
     * Launches the application by reflectively instantiating the provided subclass,
     * calling its setup() method, and then building and starting the server.
     * 
     * @param appClass the subclass of JoltApplication to launch
     * @param args     command-line arguments
     */
    public static <T extends JoltApplication> void launch(Class<T> appClass, String[] args) {
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
     * Called during launch() so that the user can configure routes and server
     * settings.
     */
    protected abstract void setup();

    // DSL-like static methods for route definitions

    protected static void get(String path, RouteHandler handler) {
        instance.router.get(path, handler);
    }

    protected static void get(String path, Supplier<Object> supplier) {
        instance.router.get(path, supplier);
    }

    protected static void post(String path, RouteHandler handler) {
        instance.router.post(path, handler);
    }

    protected static void post(String path, Supplier<Object> supplier) {
        instance.router.post(path, supplier);
    }

    protected static void put(String path, RouteHandler handler) {
        instance.router.put(path, handler);
    }

    protected static void put(String path, Supplier<Object> supplier) {
        instance.router.put(path, supplier);
    }

    protected static void delete(String path, RouteHandler handler) {
        instance.router.delete(path, handler);
    }

    protected static void delete(String path, Supplier<Object> supplier) {
        instance.router.delete(path, supplier);
    }
}