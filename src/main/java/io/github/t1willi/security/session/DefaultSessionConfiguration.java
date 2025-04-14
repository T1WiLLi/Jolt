package io.github.t1willi.security.session;

import io.github.t1willi.injector.annotation.JoltConfiguration;
import io.github.t1willi.injector.type.ConfigurationType;
import jakarta.annotation.PostConstruct;

@JoltConfiguration(value = ConfigurationType.SESSION, isDefault = true)
public class DefaultSessionConfiguration extends SessionConfig {

    @PostConstruct
    public void init() {
        configure();
    }

    @Override
    protected void configure() {
        // Configure session settings here, the default settings are already set in the
        // parent class
    }
}
