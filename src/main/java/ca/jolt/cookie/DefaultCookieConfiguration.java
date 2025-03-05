package ca.jolt.cookie;

import ca.jolt.injector.annotation.JoltConfiguration;
import ca.jolt.injector.annotation.PostConstruct;
import ca.jolt.injector.type.ConfigurationType;

@JoltConfiguration(value = ConfigurationType.COOKIE, isDefault = true)
public class DefaultCookieConfiguration extends CookieConfiguration {

    @PostConstruct
    public void init() {
        configure();
    }

    @Override
    public CookieConfiguration configure() {
        return this;
    }
}
