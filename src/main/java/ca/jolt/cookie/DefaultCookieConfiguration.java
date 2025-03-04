package ca.jolt.cookie;

import ca.jolt.injector.annotation.JoltConfiguration;
import ca.jolt.injector.type.ConfigurationType;

@JoltConfiguration(value = ConfigurationType.COOKIE, isDefault = true)
public class DefaultCookieConfiguration extends CookieConfiguration {
    @Override
    public CookieConfiguration configure() {
        return this;
    }
}
