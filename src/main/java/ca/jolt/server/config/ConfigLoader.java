package ca.jolt.server.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ConfigLoader {

    private static final Logger logger = Logger.getLogger(ConfigLoader.class.getName());
    private static final String DEFAULT_CONFIG_FILE = "/META-INF/application.properties";
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]+)\\}");

    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")
            .ignoreIfMissing()
            .load();

    public static Properties load() {
        Properties props = loadFromPropertiesFile();
        resolvePlaceholders(props);
        return props;
    }

    private static Properties loadFromPropertiesFile() {
        Properties props = new Properties();
        try (InputStream in = ConfigLoader.class.getResourceAsStream(DEFAULT_CONFIG_FILE)) {
            if (in != null) {
                props.load(in);
            } else {
                logger.warning(
                        () -> "No configuration file found at " + DEFAULT_CONFIG_FILE + ". Using default values.");
            }
        } catch (IOException e) {
            logger.severe(() -> "Error loading properties: " +
                    (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
        return props;
    }

    private static void resolvePlaceholders(Properties props) {
        if (dotenv == null) {
            logger.warning("No .env file found. Placeholders will not be resolved.");
            return;
        }

        boolean anyUnresolved;
        int iterations = 0;
        final int MAX_ITERATIONS = 10;

        do {
            anyUnresolved = false;
            iterations++;

            for (String key : props.stringPropertyNames()) {
                String value = props.getProperty(key);
                Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);

                if (matcher.find()) {
                    StringBuffer resolvedValue = new StringBuffer();
                    matcher.reset();

                    while (matcher.find()) {
                        String envKey = matcher.group(1);
                        String envValue = dotenv.get(envKey);

                        if (envValue != null) {
                            matcher.appendReplacement(resolvedValue, Matcher.quoteReplacement(envValue));
                        } else {
                            anyUnresolved = true;
                            matcher.appendReplacement(resolvedValue, Matcher.quoteReplacement(matcher.group(0)));
                        }
                    }

                    matcher.appendTail(resolvedValue);
                    props.setProperty(key, resolvedValue.toString());
                }
            }
        } while (anyUnresolved && iterations < MAX_ITERATIONS);

        if (iterations >= MAX_ITERATIONS) {
            logger.warning(
                    "Reached maximum placeholder resolution iterations. Some placeholders may remain unresolved.");
        }
        logger.info(() -> ".env file loaded and injected with success.");
    }

    /**
     * Get a specific environment variable
     */
    public static String getEnv(String key, String defaultValue) {
        String value = dotenv.get(key);
        return (value != null) ? value : defaultValue;
    }

    private ConfigLoader() {
    }
}