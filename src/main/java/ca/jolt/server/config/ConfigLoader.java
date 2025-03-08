package ca.jolt.server.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads and manages configuration properties for the application.
 * <p>
 * This utility class retrieves configuration settings from a default properties
 * file
 * ({@value #DEFAULT_CONFIG_FILE}) and resolves placeholders using environment
 * variables
 * loaded from a {@code .env} file via the {@link Dotenv} library. It provides
 * methods
 * to access the loaded properties and specific environment variables.
 */
public final class ConfigLoader {

    /**
     * Logger instance for logging configuration loading events and errors.
     */
    private static final Logger logger = Logger.getLogger(ConfigLoader.class.getName());

    /**
     * Default path to the application's properties file.
     */
    private static final String DEFAULT_CONFIG_FILE = "/META-INF/application.properties";

    /**
     * Regular expression pattern to identify placeholders in property values (e.g.,
     * {@code {VARIABLE}}).
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^{}]+)\\}");

    /**
     * Dotenv instance for loading environment variables from a {@code .env} file.
     */
    private static final Dotenv dotenv = Dotenv.configure()
            .directory("./")
            .ignoreIfMissing()
            .load();

    /**
     * Loads configuration properties from the default file and resolves
     * placeholders.
     * <p>
     * This method first loads properties from {@value #DEFAULT_CONFIG_FILE} and
     * then
     * replaces any placeholders (e.g., {@code {VARIABLE}}) with values from the
     * {@code .env} file.
     *
     * @return A {@link Properties} object containing the loaded and resolved
     *         configuration.
     */
    public static Properties load() {
        Properties props = loadFromPropertiesFile();
        resolvePlaceholders(props);
        return props;
    }

    /**
     * Loads properties from the default configuration file.
     * <p>
     * Attempts to read properties from {@value #DEFAULT_CONFIG_FILE}. If the file
     * is not found
     * or an error occurs, a warning or error is logged, and an empty
     * {@link Properties} object
     * is returned.
     *
     * @return A {@link Properties} object containing the properties loaded from the
     *         file.
     */
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

    /**
     * Resolves placeholders in property values using environment variables.
     * <p>
     * Iterates through the provided {@link Properties}, replacing any placeholders
     * (e.g.,
     * {@code {VARIABLE}}) with corresponding values from the {@code .env} file
     * loaded by {@link Dotenv}.
     * This process repeats up to 10 times to handle nested placeholders. If, after
     * the maximum
     * iterations, some placeholders remain unresolved, a warning is logged.
     *
     * @param props The {@link Properties} object whose values need placeholder
     *              resolution.
     */
    private static void resolvePlaceholders(Properties props) {
        if (dotenv == null) {
            logger.warning("No .env file found. Placeholders will not be resolved.");
            return;
        }

        final int MAX_ITERATIONS = 10;
        int iterations = 0;
        boolean anyUnresolved;

        do {
            anyUnresolved = false;
            iterations++;

            for (String key : props.stringPropertyNames()) {
                if (resolveProperty(props, key)) {
                    anyUnresolved = true;
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
     * Resolves placeholders in the property value for a specific key.
     * <p>
     * For each placeholder found in the property's value, this method attempts to
     * replace it
     * with the corresponding environment variable from {@code .env}. If a matching
     * value is found,
     * it is replaced; otherwise, the placeholder remains and the method indicates
     * that unresolved
     * placeholders exist.
     *
     * @param props The {@link Properties} object containing the property.
     * @param key   The key of the property to process.
     * @return {@code true} if any placeholders remain unresolved; {@code false}
     *         otherwise.
     */
    private static boolean resolveProperty(Properties props, String key) {
        String value = props.getProperty(key);
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);

        if (!matcher.find()) {
            return false;
        }

        StringBuffer resolvedValue = new StringBuffer();
        matcher.reset();
        boolean anyUnresolved = false;

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

        return anyUnresolved;
    }

    /**
     * Retrieves a specific environment variable with a fallback default value.
     *
     * @param key          The key of the environment variable to retrieve.
     * @param defaultValue The default value to return if the variable is not found.
     * @return The value of the environment variable, or {@code defaultValue} if not
     *         found.
     */
    public static String getEnv(String key, String defaultValue) {
        String value = dotenv.get(key);
        return (value != null) ? value : defaultValue;
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ConfigLoader() {
    }
}