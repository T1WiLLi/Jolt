package ca.jolt.core;

import java.util.function.Supplier;
import java.util.logging.Logger;

import ca.jolt.exceptions.ServerException;
import ca.jolt.injector.JoltContainer;
import ca.jolt.logging.LogConfigurator;
import ca.jolt.logging.StartupLog;
import ca.jolt.routing.RouteHandler;
import ca.jolt.server.WebServerBuilder;
import ca.jolt.server.abstraction.WebServer;

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

    protected final Router router = new Router();
    protected WebServerBuilder serverBuilder;
    protected WebServer webServer;

    protected JoltApplication() {
        if (instance != null) {
            throw new IllegalStateException("Only one JoltApplication instance is allowed per JVM");
        }
        instance = this;
        StartupLog.printStartup();
        LogConfigurator.configure();
        log.info("JoltApplication initialized");
        JoltContainer.getInstance().scan("ca.jolt").initialize();
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
            instance.setup(); // user-defined setup: routes, server configuration, etc.
            if (instance.serverBuilder == null) {
                instance.serverBuilder = new WebServerBuilder();
            }
            instance.webServer = instance.serverBuilder.build();
            instance.configureRouting(instance.webServer, instance.router);
            instance.webServer.start();
            log.info("Server started successfully!");
        } catch (Exception e) {
            log.severe("Failed to launch application: " + e.getMessage());
            System.exit(1);
        }
    }

    public static void stop() {
        if (instance != null) {
            try {
                instance.webServer.stop();
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

    /**
     * Called during launch() so that the user can bind the Router to the server.
     *
     * @param server the built WebServer instance
     * @param router the Router containing route definitions
     */
    protected abstract void configureRouting(WebServer server, Router router);

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

    /**
     * Returns the WebServerBuilder instance, creating it if necessary.
     */
    public static WebServerBuilder buildServer() {
        if (instance.serverBuilder == null) {
            instance.serverBuilder = new WebServerBuilder();
        }
        return instance.serverBuilder;
    }

    /**
     * Returns the application's Router.
     */
    public static Router getRouter() {
        return instance.router;
    }
}