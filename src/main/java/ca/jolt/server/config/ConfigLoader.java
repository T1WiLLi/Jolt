package ca.jolt.server.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

public final class ConfigLoader {

    private static final Logger logger = Logger.getLogger(ConfigLoader.class.getName());
    private static final String DEFAULT_CONFIG_FILE = "/META-INF/application.properties";

    public static Properties load() {
        Properties props = new Properties();
        try (InputStream in = ConfigLoader.class.getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (in != null) {
                props.load(in);
            } else {
                logger.warning("No configuration file found at " + DEFAULT_CONFIG_FILE + ". Using default values.");
            }
        } catch (IOException e) {
            logger.severe("Caused by: " + e.getCause().getMessage());
            StringBuilder causeStackTrace = new StringBuilder();
            for (StackTraceElement element : e.getCause().getStackTrace()) {
                causeStackTrace.append("    at ").append(element.toString()).append("\n");
            }
            logger.severe(causeStackTrace.toString());
        }
        return props;
    }
}
