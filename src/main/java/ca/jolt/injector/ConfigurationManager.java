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
     * Registers a configuration class annotated with {@link JoltConfiguration}.
     * <p>
     * If a configuration for a given type already exists:
     * <ul>
     * <li>If both the existing and the new configuration are default, a warning is
     * logged and the first is kept.</li>
     * <li>If the existing configuration is default and the new one is user-provided
     * (non-default), the new one overrides.</li>
     * <li>If the existing configuration is non-default and the new one is default,
     * a warning is logged and the new one is ignored.</li>
     * <li>If both are non-default, a warning is logged and the first registered
     * configuration is used.</li>
     * </ul>
     * </p>
     *
     * @param configClass the configuration class to register.
     * @throws JoltDIException if the class is not annotated with
     *                         {@code @JoltConfiguration} or if it doesn't meet the
     *                         required rules.
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
        // Specific validation for exception handler configurations.
        if (type == ConfigurationType.EXCEPTION_HANDLER) {
            if (!GlobalExceptionHandler.class.isAssignableFrom(configClass)) {
                throw new JoltDIException(
                        "Configuration for EXCEPTION_HANDLER must implement GlobalExceptionHandler: " +
                                configClass.getName());
            }
        }
        try {
            Object configInstance = configClass.getDeclaredConstructor().newInstance();
            if (configurations.containsKey(type)) {
                Object current = configurations.get(type);
                boolean currentIsDefault = current.getClass()
                        .getAnnotation(JoltConfiguration.class).isDefault();
                boolean newIsDefault = configAnno.isDefault();
                if (currentIsDefault && newIsDefault) {
                    logger.warning("Duplicate default configuration detected for type " + type +
                            ". Using the first registered configuration: " + current.getClass().getName());
                } else if (currentIsDefault && !newIsDefault) {
                    configurations.put(type, configInstance);
                    logger.info("Overriding default configuration " + current.getClass().getName() +
                            " with user provided configuration " + configClass.getName() + " for type " + type);
                } else if (!currentIsDefault && newIsDefault) {
                    logger.warning("Default configuration " + configClass.getName() +
                            " ignored since a non-default configuration is already registered for type " + type);
                } else {
                    logger.warning("Multiple non-default configuration beans registered for type " + type +
                            ". Using the first registered configuration: " + current.getClass().getName());
                }
            } else {
                configurations.put(type, configInstance);
                logger.info("Registered configuration " + configClass.getName() + " for type " + type);
            }
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
