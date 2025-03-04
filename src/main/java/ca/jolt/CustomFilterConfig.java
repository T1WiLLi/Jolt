package ca.jolt;

import ca.jolt.filters.FilterConfiguration;
import ca.jolt.injector.annotation.JoltConfiguration;
import ca.jolt.injector.type.ConfigurationType;

@JoltConfiguration(value = ConfigurationType.FILTER)
public class CustomFilterConfig extends FilterConfiguration {

    @Override
    public void configure() {
        exclude("/");
    }
}
