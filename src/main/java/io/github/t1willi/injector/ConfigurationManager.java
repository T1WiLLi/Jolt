package io.github.t1willi.injector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import io.github.t1willi.exceptions.JoltDIException;
import io.github.t1willi.exceptions.handler.GlobalExceptionHandler;
import io.github.t1willi.filters.FilterConfiguration;
import io.github.t1willi.injector.annotation.Configuration;
import io.github.t1willi.injector.type.ConfigurationType;
import io.github.t1willi.security.config.SecurityConfiguration;
import jakarta.annotation.PostConstruct;

/**
 * The ConfigurationManager is responsible for registering and validating
 * configuration beans
 * annotated with {@link Configuration}. It enforces rules based on the
 * configuration type and
 * applies automatic validation and registering of the configurations in the
 * correct emplacement for the injector.
 */
final class ConfigurationManager {

    private static final Logger logger = Logger.getLogger(ConfigurationManager.class.getName());
    private final Map<ConfigurationType, Object> configurations = new EnumMap<>(ConfigurationType.class);

    /**
     * Registers a configuration class annotated with {@link Configuration}.
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
     *
     * @param configClass the configuration class to register.
     * @throws JoltDIException if the class is not annotated with
     *                         {@code @Configuration} or if it doesn't meet the
     *                         required rules.
     */
    public void registerConfiguration(Class<?> configClass) {
        Objects.requireNonNull(configClass, "Configuration class cannot be null");
        validateConfigurationClass(configClass);
        ConfigurationType type = configClass.getAnnotation(Configuration.class).value();
        validateConfigurationType(configClass, type);
        Object configInstance = createConfigurationInstance(configClass);
        handleConfigurationRegistration(configClass, type, configInstance);
    }

    public void initializeConfigurations() {
        for (Object config : configurations.values()) {
            invokeLifecycleMethod(config, PostConstruct.class);
        }
        logger.info("Configuration beans initialized.");
    }

    private void invokeLifecycleMethod(Object instance, Class<? extends Annotation> annotationType) {
        Class<?> clazz = instance.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(annotationType)) {
                try {
                    method.invoke(instance);
                } catch (Exception e) {
                    throw new JoltDIException("Failed to invoke lifecycle method on: " + clazz.getName(), e);
                }
            }
        }
    }

    private void validateConfigurationClass(Class<?> configClass) {
        if (!configClass.isAnnotationPresent(Configuration.class)) {
            throw new JoltDIException("Configuration class " + configClass.getName() +
                    " is not annotated with @Configuration");
        }
    }

    /**
     * Validates the configuration class against the provided configuration type.
     * <p>
     * This method checks that:
     * <ol>
     * <li>A configuration type is provided.</li>
     * <li>The configuration class implements the required interface for the given
     * type.</li>
     * <li>For all types except EXCEPTION_HANDLER, the configuration class defines
     * the required lifecycle methods.</li>
     * </ol>
     *
     * @param configClass The configuration class to validate.
     * @param type        The configuration type.
     * @throws JoltDIException If any validation fails.
     */
    private void validateConfigurationType(Class<?> configClass, ConfigurationType type) {
        if (type == null) {
            throw new JoltDIException("Configuration type must be provided in @Configuration for " +
                    configClass.getName());
        }
        validateRequiredInterface(configClass, type);

        if (type != ConfigurationType.EXCEPTION_HANDLER) {
            validateLifecycleMethods(configClass);
        }
    }

    /**
     * Validates that the given configuration class implements the required
     * interface
     * for the specified configuration type.
     *
     * @param configClass The configuration class to validate.
     * @param type        The configuration type.
     * @throws JoltDIException If the configuration class does not implement the
     *                         required interface.
     */
    private void validateRequiredInterface(Class<?> configClass, ConfigurationType type) {
        switch (type) {
            case EXCEPTION_HANDLER:
                if (!GlobalExceptionHandler.class.isAssignableFrom(configClass)) {
                    throw new JoltDIException(
                            "Configuration for EXCEPTION_HANDLER must implement GlobalExceptionHandler: " +
                                    configClass.getName());
                }
                break;
            case FILTER:
                if (!FilterConfiguration.class.isAssignableFrom(configClass)) {
                    throw new JoltDIException("Configuration for FILTER must implement FilterConfiguration: " +
                            configClass.getName());
                }
                break;
            case SECURITY:
                if (!SecurityConfiguration.class.isAssignableFrom(configClass)) {
                    throw new JoltDIException("Configuration for SECURITY must implement SecurityConfiguration: " +
                            configClass.getName());
                }
                break;
            default:
                break;
        }
    }

    /**
     * Validates that the configuration class defines the required lifecycle
     * methods.
     * <p>
     * Specifically, this method verifies that:
     * <ul>
     * <li>The <code>init()</code> method exists and is annotated with
     * <code>@PostConstruct</code>.</li>
     * <li>The <code>configure()</code> method exists.</li>
     * </ul>
     *
     * @param configClass The configuration class to validate.
     * @throws JoltDIException If the required lifecycle methods are missing or
     *                         incorrectly annotated.
     */
    private void validateLifecycleMethods(Class<?> configClass) {
        try {
            Method initMethod = configClass.getDeclaredMethod("init");
            if (!initMethod.isAnnotationPresent(PostConstruct.class)) {
                throw new JoltDIException("Configuration class must have @PostConstruct annotation on init() method: " +
                        configClass.getName());
            }
        } catch (NoSuchMethodException e) {
            throw new JoltDIException("Configuration class must implement init() method with @PostConstruct: " +
                    configClass.getName());
        }

        try {
            configClass.getDeclaredMethod("configure");
        } catch (NoSuchMethodException e) {
            throw new JoltDIException("Configuration class must implement configure() method: " +
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
            boolean currentIsDefault = current.getClass().getAnnotation(Configuration.class).isDefault();
            boolean newIsDefault = configClass.getAnnotation(Configuration.class).isDefault();
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
                boolean isDefault = config.getClass().getAnnotation(Configuration.class).isDefault();
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
