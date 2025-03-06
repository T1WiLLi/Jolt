package ca.jolt.security.config;

import ca.jolt.injector.annotation.JoltConfiguration;
import ca.jolt.injector.annotation.PostConstruct;
import ca.jolt.injector.type.ConfigurationType;
import ca.jolt.security.policies.FrameOptionsPolicy;
import ca.jolt.security.policies.HstsPolicy;
import ca.jolt.security.policies.ReferrerPolicy;
import ca.jolt.security.policies.XssProtectionPolicy;

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
                .withXssProtection(XssProtectionPolicy.ENABLE_BLOCK)
                .withFrameOptions(FrameOptionsPolicy.DENY)
                .withHsts(HstsPolicy.ONE_YEAR_WITH_SUBDOMAINS_PRELOAD)
                .withReferrerPolicy(ReferrerPolicy.SAME_ORIGIN)
                .httpsOnly(false)
                .contentSecurityPolicy(true);
        return this;
    }
}
