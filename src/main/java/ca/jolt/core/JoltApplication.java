package ca.jolt.core;

import java.util.function.Supplier;
import java.util.logging.Logger;

import ca.jolt.exceptions.ServerException;
import ca.jolt.logging.LogConfigurator;
import ca.jolt.logging.StartupLog;
import ca.jolt.routing.RouteHandler;
import ca.jolt.tomcat.TomcatServer;
import ca.jolt.tomcat.WebServerBuilder;
import ca.jolt.tomcat.abstraction.WebServer;

/**
 * Base class for a Jolt application.
 * 
 * Usage:
 * public class MyApp extends JoltApplication {
 * public static void main(String[] args) {
 * new MyApp().run(args);
 * }
 *
 * @Override
 *           protected void configureRouting(WebServer server, Router router) {
 *           server.setRouter(router);
 *           }
 *           }
 */
public abstract class JoltApplication {

    private static final Logger log = Logger.getLogger(JoltApplication.class.getName());

    private static JoltApplication instance;

    protected final Router router = new Router();
    protected WebServerBuilder serverBuilder;
    protected WebServer webServer;

    // If user wants to override the 404 or 500 pages
    private boolean customNotFound = false;
    private boolean customError = false;

    protected JoltApplication() {
        if (instance == null) {
            StartupLog.printStartup();
            LogConfigurator.configure();
        }
        if (instance != null) {
            throw new IllegalStateException("Only one JoltApplication instance is allowed per JVM");
        }
        instance = this;
        log.info("JoltApplication initialized");
    }

    /**
     * Optionally call from main(...) to start your app.
     * If you prefer the DSL approach (buildServer, get, etc.), you can do so before
     * run().
     */
    public void run(String[] args) {
        // If no server built, build a default
        if (serverBuilder == null) {
            serverBuilder = new WebServerBuilder();
        }
        start();
    }

    /**
     * Configure the server to use custom or default error pages.
     */
    public void useCustomNotFound(boolean enabled) {
        this.customNotFound = enabled;
    }

    public void useCustomError(boolean enabled) {
        this.customError = enabled;
    }

    /**
     * The subclass must implement how to link the Router to the server
     * (e.g., server.setRouter(router)).
     */
    protected abstract void configureRouting(WebServer server, Router router);

    /*
     * -----------------------------------------------------------------
     * DSL-like static methods for route definitions
     * -----------------------------------------------------------------
     */
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

    /*
     * -----------------------------------------------------------------
     * Build / Start server
     * -----------------------------------------------------------------
     */
    public static WebServerBuilder buildServer() {
        if (instance.serverBuilder == null) {
            instance.serverBuilder = new WebServerBuilder();
        }
        return instance.serverBuilder;
    }

    public static void start() {
        if (instance.serverBuilder == null) {
            throw new IllegalStateException("Call buildServer() before start() to configure the server.");
        }
        try {
            instance.webServer = instance.serverBuilder.build();

            // Link the router to the server
            instance.configureRouting(instance.webServer, instance.router);

            // The server (e.g., TomcatServer) should register a JoltDispatcherServlet
            // with "useCustomNotFound" and "useCustomError" settings
            if (instance.webServer instanceof TomcatServer) {
                TomcatServer tserver = (TomcatServer) instance.webServer;
                tserver.setCustomErrorPages(instance.customNotFound, instance.customError);
            }

            instance.webServer.start();
            log.info("Server started successfully!");
        } catch (ServerException e) {
            log.severe("Failed to start server: " + e.getMessage());
        }
    }

    public static Router getRouter() {
        return instance.router;
    }
}