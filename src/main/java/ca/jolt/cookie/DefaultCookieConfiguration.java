package ca.jolt.cookie;

import ca.jolt.injector.annotation.JoltConfiguration;
import ca.jolt.injector.annotation.PostConstruct;
import ca.jolt.injector.type.ConfigurationType;

/**
 * Provides a default cookie configuration that is automatically loaded.
 * <p>
 * This configuration extends {@link CookieConfiguration} and is annotated with
 * {@code @JoltConfiguration} to indicate that it is the default cookie
 * configuration for the application.
 */
@JoltConfiguration(value = ConfigurationType.COOKIE, isDefault = true)
public class DefaultCookieConfiguration extends CookieConfiguration {

    /**
     * Initializes this default cookie configuration by invoking
     * {@link #configure()}.
     */
    @PostConstruct
    public void init() {
        configure();
    }

    /**
     * Completes any necessary configuration steps for this default cookie
     * configuration.
     *
     * @return This configuration instance for chaining
     */
    @Override
    public CookieConfiguration configure() {
        return this;
    }
}
