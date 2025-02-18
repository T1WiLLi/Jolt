package ca.jolt.logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * LogConfigurator is responsible for configuring the Java Util Logging (JUL)
 * framework
 * to use a custom formatter {@link LogFormatter} and setting up the logging
 * levels
 * and handlers for various loggers.
 * 
 * <p>
 * This class sets the logging manager to
 * {@code org.apache.juli.ClassLoaderLogManager}
 * and configures the logging settings programmatically. It removes existing
 * handlers
 * from the root logger and adds a {@link ConsoleHandler} with a custom
 * {@link LogFormatter}.
 * Additionally, it configures specific loggers for Apache Tomcat components and
 * the
 * application's package to use the same handler and formatter.
 * </p>
 * 
 * <p>
 * Usage:
 * </p>
 * 
 * <pre>
 * {@code
 * LogConfigurator.configure();
 * }
 * </pre>
 * 
 * <p>
 * Example:
 * </p>
 * 
 * <pre>
 * {@code
 * public class Main {
 *     public static void main(String[] args) {
 *         LogConfigurator.configure();
 *         Logger logger = Logger.getLogger(Main.class.getName());
 *         logger.info("Application started");
 *     }
 * }
 * </pre>
 * 
 * @author William Beaudin
 * 
 * @since 1.0
 */
public class LogConfigurator {
    public static void configure() {
        try {
            System.setProperty("java.util.logging.manager", "org.apache.juli.ClassLoaderLogManager");
            System.setProperty("java.util.logging.config.class", "ca.jolt.logging.LogConfigurator");

            LogManager logManager = LogManager.getLogManager();
            logManager.reset();

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new LogFormatter());
            consoleHandler.setLevel(Level.INFO);

            Logger rootLogger = Logger.getLogger("");
            rootLogger.setLevel(Level.INFO);

            for (Handler handler : rootLogger.getHandlers()) {
                rootLogger.removeHandler(handler);
            }

            rootLogger.addHandler(consoleHandler);

            String[] loggers = {
                    "org.apache.catalina",
                    "org.apache.coyote",
                    "org.apache.tomcat",
                    "ca.jolt"
            };

            for (String loggerName : loggers) {
                Logger logger = Logger.getLogger(loggerName);
                logger.setLevel(Level.INFO);
                for (Handler handler : logger.getHandlers()) {
                    logger.removeHandler(handler);
                }
                logger.addHandler(consoleHandler);
                logger.setUseParentHandlers(false);
            }

            Logger.getLogger("Main").info("Logging system initialized successfully");

        } catch (Exception e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
            e.printStackTrace();
        }
    }
}