package ca.jolt.injector;

import java.util.EnumMap;
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
final class ConfigurationManager {

    private static final Logger logger = Logger.getLogger(ConfigurationManager.class.getName());
    private final Map<ConfigurationType, Object> configurations = new EnumMap<>(ConfigurationType.class);

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
        validateConfigurationClass(configClass);
        ConfigurationType type = configClass.getAnnotation(JoltConfiguration.class).value();
        validateConfigurationType(configClass, type);
        Object configInstance = createConfigurationInstance(configClass);
        handleConfigurationRegistration(configClass, type, configInstance);
    }

    private void validateConfigurationClass(Class<?> configClass) {
        if (!configClass.isAnnotationPresent(JoltConfiguration.class)) {
            throw new JoltDIException("Configuration class " + configClass.getName() +
                    " is not annotated with @JoltConfiguration");
        }
    }

    private void validateConfigurationType(Class<?> configClass, ConfigurationType type) {
        if (type == null) {
            throw new JoltDIException("Configuration type must be provided in @JoltConfiguration for " +
                    configClass.getName());
        }
        if (type == ConfigurationType.EXCEPTION_HANDLER
                && !GlobalExceptionHandler.class.isAssignableFrom(configClass)) {
            throw new JoltDIException(
                    "Configuration for EXCEPTION_HANDLER must implement GlobalExceptionHandler: " +
                            configClass.getName());
        }
    }

    private Object createConfigurationInstance(Class<?> configClass) {
        try {
            return configClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new JoltDIException("Failed to instantiate configuration: " + configClass.getName(), e);
        }
    }

    private void handleConfigurationRegistration(Class<?> configClass, ConfigurationType type, Object configInstance) {
        if (configurations.containsKey(type)) {
            Object current = configurations.get(type);
            boolean currentIsDefault = current.getClass().getAnnotation(JoltConfiguration.class).isDefault();
            boolean newIsDefault = configClass.getAnnotation(JoltConfiguration.class).isDefault();
            handleExistingConfiguration(configClass, type, configInstance, current, currentIsDefault, newIsDefault);
        } else {
            configurations.put(type, configInstance);
            logger.info(() -> "Registered configuration " + configClass.getName() + " for type " + type);
        }
    }

    private void handleExistingConfiguration(Class<?> configClass, ConfigurationType type, Object configInstance,
            Object current, boolean currentIsDefault, boolean newIsDefault) {
        if (currentIsDefault && newIsDefault) {
            logger.warning(() -> "Duplicate default configuration detected for type " + type +
                    ". Using the first registered configuration: " + current.getClass().getName());
        } else if (currentIsDefault) {
            configurations.put(type, configInstance);
            logger.info(() -> "Overriding default configuration " + current.getClass().getName() +
                    " with user provided configuration " + configClass.getName() + " for type " + type);
        } else if (newIsDefault) {
            logger.warning(() -> "Default configuration " + configClass.getName() +
                    " ignored since a non-default configuration is already registered for type " + type);
        } else {
            logger.warning(() -> "Multiple non-default configuration beans registered for type " + type +
                    ". Using the first registered configuration: " + current.getClass().getName());
        }
    }

    /**
     * Retrieves a configuration bean by its configuration type.
     *
     * @param expectedType the expected class of the configuration.
     * @param <T>          the type parameter.
     * @return the configuration instance.
     * @throws JoltDIException if no configuration is registered for the given type.
     */
    public <T> T getConfiguration(Class<T> expectedType) {
        T defaultConfig = null;
        for (Object config : configurations.values()) {
            if (expectedType.isAssignableFrom(config.getClass())) {
                boolean isDefault = config.getClass().getAnnotation(JoltConfiguration.class).isDefault();
                if (!isDefault) {
                    return expectedType.cast(config);
                } else {
                    defaultConfig = expectedType.cast(config);
                }
            }
        }

        if (defaultConfig != null) {
            return defaultConfig;
        }

        throw new JoltDIException("No configuration registered for type: " + expectedType.getName());
    }

    /**
     * Clears all registered configurations.
     */
    public void clear() {
        configurations.clear();
    }
}
