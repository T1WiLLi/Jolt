package ca.jolt.security.config;

import ca.jolt.injector.annotation.JoltConfiguration;
import ca.jolt.injector.type.ConfigurationType;
import ca.jolt.security.policies.FrameOptionsPolicy;
import ca.jolt.security.policies.HstsPolicy;
import ca.jolt.security.policies.ReferrerPolicy;
import ca.jolt.security.policies.XssProtectionPolicy;
import jakarta.annotation.PostConstruct;

/**
 * Default implementation of the SecurityConfiguration.
 * Provides standard security settings for a Jolt application with sensible
 * defaults.
 * This configuration includes CORS settings and security headers configuration.
 */
@JoltConfiguration(value = ConfigurationType.SECURITY, isDefault = true)
public class DefaultSecurityConfiguration extends SecurityConfiguration {

    /**
     * Initializes this configuration when the bean is constructed.
     * This method is automatically called by the container due to
     * the @PostConstruct annotation.
     */
    @PostConstruct
    public void init() {
        configure();
    }

    /**
     * Configures security settings with default values.
     * <p>
     * Default CORS configuration:
     * - Allows requests from any origin
     * - Allows GET, POST, PUT, DELETE methods
     * - Allows Origin, Content-Type, Accept, and Authorization headers
     * - Disables credentials
     * - Sets max age to 3600 seconds (1 hour)
     * <p>
     * Default security headers:
     * - Enables XSS protection with blocking mode
     * - Sets X-Frame-Options to DENY
     * - Configures HSTS for one year with subdomains and preload
     * - Sets Referrer-Policy to same-origin
     * - Doesn't enforce HTTPS only
     * - Enables Content-Security-Policy
     *
     * @return This configuration instance for method chaining
     */
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