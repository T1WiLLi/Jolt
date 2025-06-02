package io.github.t1willi.injector;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import io.github.t1willi.exceptions.JoltDIException;
import io.github.t1willi.exceptions.handler.GlobalExceptionHandler;
import io.github.t1willi.injector.annotation.Configuration;

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
    private final Map<Class<?>, Object> configurations = new HashMap<>();

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
        Object configInstance = createConfigurationInstance(configClass);
        handleConfigurationRegistration(configClass, configInstance);
    }

    public void initializeConfigurations() {
        for (Object config : configurations.values()) {
            invokeConfigureMethod(config);
        }
        logger.info("Configuration beans initialized.");
    }

    private void invokeConfigureMethod(Object instance) {
        Class<?> clazz = instance.getClass();
        if (GlobalExceptionHandler.class.isAssignableFrom(clazz)) {
            logger.fine(() -> "Skipping configure() invocation for " + clazz.getName());
            return;
        }

        try {
            Method configureMethod = clazz.getMethod("configure");
            if (!configureMethod.getReturnType().equals(void.class)
                    || !Modifier.isPublic(configureMethod.getModifiers())) {
                throw new JoltDIException(
                        "Configuration class" + clazz.getName() + " must have a public void configure() method");
            }
            configureMethod.invoke(instance);
        } catch (NoSuchMethodException e) {
            throw new JoltDIException(
                    "Configuration class " + clazz.getName() + " must have a public void configure() method - "
                            + e.getMessage());
        } catch (Exception e) {
            throw new JoltDIException(
                    "Failed to invoke configure() method on: " + clazz.getName() + " - " + e.getMessage(), e);
        }
    }

    private void validateConfigurationClass(Class<?> configClass) {
        if (!configClass.isAnnotationPresent(Configuration.class)) {
            throw new JoltDIException("Configuration class " + configClass.getName() +
                    " is not annotated with @Configuration");
        }
    }

    private void handleConfigurationRegistration(Class<?> configClass, Object configInstance) {
        if (configurations.containsKey(configClass)) {
            Object current = configurations.get(configClass);
            boolean currentIsDefault = current.getClass().getAnnotation(Configuration.class).isDefault();
            boolean newIsDefault = configClass.getAnnotation(Configuration.class).isDefault();
            handleExistingConfiguration(configClass, configInstance, current, currentIsDefault, newIsDefault);
        } else {
            configurations.put(configClass, configInstance);
            logger.info(() -> "Registered configuration " + configClass.getName());
        }
    }

    private void handleExistingConfiguration(Class<?> configClass, Object configInstance,
            Object current, boolean currentIsDefault, boolean newIsDefault) {
        if (currentIsDefault && newIsDefault) {
            logger.warning(() -> "Duplicate default configuration detected for class " + configClass.getName() +
                    ". Using the first registered configuration: " + current.getClass().getName());
        } else if (currentIsDefault) {
            configurations.put(configClass, configInstance);
            logger.info(() -> "Overriding default configuration " + current.getClass().getName() +
                    " with user provided configuration " + configClass.getName());
        } else if (newIsDefault) {
            logger.warning(() -> "Default configuration " + configClass.getName() +
                    " ignored since a non-default configuration is already registered for class "
                    + configClass.getName());
        } else {
            logger.warning(
                    () -> "Multiple non-default configuration beans registered for class " + configClass.getName() +
                            ". Using the first registered configuration: " + current.getClass().getName());
        }
    }

    private Object createConfigurationInstance(Class<?> configClass) {
        try {
            return configClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new JoltDIException("Failed to instantiate configuration: " + configClass.getName(), e);
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
