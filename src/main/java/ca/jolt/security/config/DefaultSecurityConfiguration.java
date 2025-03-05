package ca.jolt.security.config;

import ca.jolt.injector.annotation.JoltConfiguration;
import ca.jolt.injector.annotation.PostConstruct;
import ca.jolt.injector.type.ConfigurationType;

@JoltConfiguration(value = ConfigurationType.SECURITY, isDefault = true)
public class DefaultSecurityConfiguration extends SecurityConfiguration {

    @PostConstruct
    public void init() {
        configure();
    }

    @Override
    public SecurityConfiguration configure() {
        withCORS()
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE")
                .allowedHeaders("Origin", "Content-Type", "Accept", "Authorization")
                .allowedCredentials(false)
                .maxAge(3600);

        withHeaders()
                .withXssProtection(true)
                .denyFrameOption(true)
                .httpStrictTransportSecurity(true)
                .referrerPolicy("same-origin")
                .httpsOnly(false)
                .contentSecurityPolicy(true);
        return this;
    }
}
