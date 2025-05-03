package io.github.t1willi.filters;

import io.github.t1willi.injector.annotation.Configuration;
import io.github.t1willi.injector.type.ConfigurationType;
import jakarta.annotation.PostConstruct;

/**
 * Provides the default filter configuration for the Jolt framework.
 * <p>
 * This class is annotated with {@link JoltConfiguration} to indicate that it
 * serves as the primary filter configuration. Applications can override the
 * default behavior by defining their own configuration class with
 * {@code @JoltConfiguration(value = ConfigurationType.FILTER)}.
 * <p>
 * The {@link #configure()} method is called during filter setup and can be
 * overridden in custom implementations to provide additional logic.
 *
 * @since 1.0
 */
@Configuration(value = ConfigurationType.FILTER, isDefault = true)
public final class DefaultFilterConfiguration extends FilterConfiguration {

    @PostConstruct
    public void init() {
        configure();
    }

    /**
     * Performs filter configuration steps for the default setup.
     * <p>
     * This default implementation does not include additional logic.
     */
    @Override
    public void configure() {
        // No-op
    }
}
