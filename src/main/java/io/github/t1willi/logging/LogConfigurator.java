package io.github.t1willi.logging;

import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import io.github.t1willi.server.config.ConfigurationManager;

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

    private static final String DEFAULT_LOG_LEVEL = "INFO";

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

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new LogFormatter());
            String logLevel = ConfigurationManager.getInstance().getProperty("server.logging.level");
            consoleHandler.setLevel(Level.parse(logLevel != null ? logLevel.toUpperCase() : DEFAULT_LOG_LEVEL));

            String logFile = ConfigurationManager.getInstance().getProperty("server.logging.logfile");
            Handler fileHandler = null;
            if (logFile != null && !logFile.trim().isEmpty()) {
                fileHandler = new FileHandler(new File(logFile).getAbsolutePath(), true);
                fileHandler.setFormatter(new LogFormatter());
                fileHandler.setLevel(Level.ALL);
                fileHandler.setFilter(new Filter() {
                    @Override
                    public boolean isLoggable(LogRecord record) {
                        return record.getLevel() != Level.FINER;
                    }
                });
            }

            for (String loggerName : DEFAULT_LOGGERS) {
                configureLogger(Logger.getLogger(loggerName), consoleHandler, fileHandler);
            }

            Logger.getLogger(LogConfigurator.class.getName())
                    .info("Jolt logging system initialized successfully");

        } catch (Exception e) {
            throw new LoggingConfigurationException("Failed to initialize logging system", e);
        }
    }

    private static void configureLogger(Logger logger, Handler consoleHandler, Handler fileHandler) {
        for (Handler h : logger.getHandlers()) {
            logger.removeHandler(h);
        }

        logger.setLevel(Level.ALL);
        logger.addHandler(consoleHandler);
        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false);
    }
}