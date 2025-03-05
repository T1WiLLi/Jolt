package ca.jolt.filters;

import ca.jolt.injector.annotation.JoltConfiguration;
import ca.jolt.injector.type.ConfigurationType;

@JoltConfiguration(value = ConfigurationType.FILTER, isDefault = true)
public final class DefaultFilterConfiguration extends FilterConfiguration {

    @Override
    public void configure() {

    }
}
