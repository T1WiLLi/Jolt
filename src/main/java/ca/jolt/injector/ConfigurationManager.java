package ca.jolt.injector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import ca.jolt.exceptions.JoltDIException;
import ca.jolt.exceptions.handler.GlobalExceptionHandler;
import ca.jolt.injector.annotation.JoltConfiguration;
import ca.jolt.injector.type.ConfigurationType;

/**
 * The ConfigurationManager is responsible for registering and validating
 * configuration beans
 * annotated with {@link JoltConfiguration}. It enforces rules based on the
 * configuration type and
 * applies automatic validation and registering of the configurations in the
 * correct emplacement for the injector.
 */
public class ConfigurationManager {

    private static final Logger logger = Logger.getLogger(ConfigurationManager.class.getName());
    private final Map<ConfigurationType, Object> configurations = new HashMap<>();

    /**
     * Registers a configuration class.
     *
     * @param configClass the configuration class annotated with
     *                    {@link JoltConfiguration}.
     * @throws JoltDIException if the configuration class is not properly annotated
     *                         or does not meet the rules.
     */
    public void registerConfiguration(Class<?> configClass) {
        Objects.requireNonNull(configClass, "Configuration class cannot be null");
        if (!configClass.isAnnotationPresent(JoltConfiguration.class)) {
            throw new JoltDIException("Configuration class " + configClass.getName() +
                    " is not annotated with @JoltConfiguration");
        }
        JoltConfiguration configAnno = configClass.getAnnotation(JoltConfiguration.class);
        ConfigurationType type = configAnno.value();
        if (type == null) {
            throw new JoltDIException("Configuration type must be provided in @JoltConfiguration for " +
                    configClass.getName());
        }
        // Enforce specific rules: for example, if type is EXCEPTION_HANDLER, the class
        // must implement GlobalExceptionHandler.
        if (type == ConfigurationType.EXCEPTION_HANDLER) {
            if (!GlobalExceptionHandler.class.isAssignableFrom(configClass)) {
                throw new JoltDIException(
                        "Configuration for EXCEPTION_HANDLER must implement GlobalExceptionHandler: " +
                                configClass.getName());
            }
        }
        try {
            Object configInstance = configClass.getDeclaredConstructor().newInstance();
            configurations.put(type, configInstance);
            logger.info("Registered configuration " + configClass.getName() + " for type " + type);
        } catch (Exception e) {
            throw new JoltDIException("Failed to instantiate configuration: " + configClass.getName(), e);
        }
    }

    /**
     * Retrieves a configuration bean by its configuration type.
     *
     * @param type         the configuration type.
     * @param expectedType the expected class of the configuration.
     * @param <T>          the type parameter.
     * @return the configuration instance.
     * @throws JoltDIException if no configuration is registered for the given type.
     */
    public <T> T getConfiguration(ConfigurationType type, Class<T> expectedType) {
        Objects.requireNonNull(type, "Configuration type cannot be null");
        Object config = configurations.get(type);
        if (config == null) {
            throw new JoltDIException("No configuration registered for type: " + type);
        }
        return expectedType.cast(config);
    }

    /**
     * Clears all registered configurations.
     */
    public void clear() {
        configurations.clear();
    }
}
