package io.github.t1willi.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configures the logging system for the Jolt framework. This class sets up
 * a unified logging format for both application and Tomcat logs.
 * 
 * <p>
 * The configuration:
 * <ul>
 * <li>Sets the log level to INFO by default</li>
 * <li>Applies the {@link io.github.logging.LogFormatter} to all logs</li>
 * <li>Configures both application and Tomcat logging</li>
 * <li>Removes existing handlers to ensure consistent formatting</li>
 * </ul>
 * 
 * <p>
 * Basic usage example:
 * 
 * <pre>{@code
 * // Logging will be automatically configured on startup
 * // Then you can use standard Java logging:
 * Logger logger = Logger.getLogger(YourClass.class.getName());
 * logger.info("Application message");
 * logger.warning("Warning message");
 * 
 * // With exceptions:
 * try {
 *     // Some code
 * } catch (Exception e) {
 *     logger.log(Level.SEVERE, "Error occurred", e);
 * }
 * }</pre>
 * 
 * @author William Beaudin
 * @see LogFormatter
 * @see LogInitializer
 * @since 1.0
 */
public final class LogConfigurator {

    private static final String[] DEFAULT_LOGGERS = {
            "", // Root logger
            "org.apache.catalina", // Tomcat
            "org.apache.coyote", // Tomcat
            "org.apache.tomcat", // Tomcat
            "org.apache.jasper", // Tomcat
            "ca.jolt", // Jolt
            "com.zaxxer.hikari", // HikariCP
            "org.slf4j"
    };

    private LogConfigurator() {
        // Prevent instantiation
    }

    /**
     * Configures the logging system with Jolt's default settings.
     * This method is typically called automatically by {@link LogInitializer}
     * but can be called manually if needed.
     * 
     * @throws LoggingConfigurationException if configuration fails
     */
    public static void configure() {
        try {
            System.setProperty("java.util.logging.manager", "org.apache.juli.ClassLoaderLogManager");
            System.setProperty("java.util.logging.config.class", "ca.jolt.logging.LogConfigurator");

            ConsoleHandler handler = new ConsoleHandler();
            handler.setFormatter(new LogFormatter());
            handler.setLevel(Level.INFO);

            for (String loggerName : DEFAULT_LOGGERS) {
                configureLogger(Logger.getLogger(loggerName), handler);
            }

            Logger.getLogger(LogConfigurator.class.getName())
                    .info("Jolt logging system initialized successfully");

        } catch (Exception e) {
            throw new LoggingConfigurationException("Failed to initialize logging system", e);
        }
    }

    private static void configureLogger(Logger logger, Handler handler) {
        for (Handler h : logger.getHandlers()) {
            logger.removeHandler(h);
        }

        logger.setLevel(Level.INFO);
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);
    }
}