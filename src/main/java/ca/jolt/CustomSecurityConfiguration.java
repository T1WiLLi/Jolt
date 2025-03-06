package ca.jolt;

import ca.jolt.injector.annotation.JoltConfiguration;
import ca.jolt.injector.annotation.PostConstruct;
import ca.jolt.injector.type.ConfigurationType;
import ca.jolt.security.config.SecurityConfiguration;

@JoltConfiguration(value = ConfigurationType.SECURITY)
public class CustomSecurityConfiguration extends SecurityConfiguration {

    @PostConstruct
    public void init() {
        configure();
    }

    @Override
    protected SecurityConfiguration configure() {
        withHeaders()
                .contentSecurityPolicy("default-src 'self'; " +
                        "script-src 'self' https://cdn.tailwindcss.com https://cdnjs.cloudflare.com https://cdn.jsdelivr.net; "
                        +
                        "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com https://cdn.jsdelivr.net https://fonts.googleapis.com; "
                        +
                        "img-src 'self' data:; " +
                        "connect-src 'self';");
        return this;
    }
}
