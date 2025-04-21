package io.github.t1willi.security.config;

import io.github.t1willi.injector.annotation.JoltConfiguration;
import io.github.t1willi.injector.type.ConfigurationType;
import io.github.t1willi.security.policies.CacheControlPolicy;
import io.github.t1willi.security.policies.ContentSecurityPolicy;
import io.github.t1willi.security.policies.FrameOptionsPolicy;
import io.github.t1willi.security.policies.HstsPolicy;
import io.github.t1willi.security.policies.ReferrerPolicy;
import io.github.t1willi.security.policies.XssProtectionPolicy;
import jakarta.annotation.PostConstruct;

/**
 * Default implementation of the SecurityConfiguration.
 * Provides standard security settings for a Jolt application with sensible
 * defaults.
 * This configuration includes CORS settings and security headers configuration.
 */
@JoltConfiguration(value = ConfigurationType.SECURITY, isDefault = true)
public final class DefaultSecurityConfiguration extends SecurityConfiguration {

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
     * <ul>
     * <li>Allows requests from any origin</li>
     * <li>Allows GET, POST, PUT, DELETE methods</li>
     * <li>Allows Origin, Content-Type, Accept, and Authorization headers</li>
     * <li>Disables credentials</li>
     * <li>Sets max age to 3600 seconds (1 hour)</li>
     * </ul>
     * <p>
     * Default security headers:
     * <ul>
     * <li>Enables XSS protection with blocking mode</li>
     * <li>Sets X-Frame-Options to DENY</li>
     * <li>Configures HSTS for one year with subdomains and preload</li>
     * <li>Sets Referrer-Policy to same-origin</li>
     * <li>Doesn't enforce HTTPS only</li>
     * <li>Enables Content-Security-Policy</li>
     * <li>Sets Cache-Control to no-cache</li>
     * </ul>
     * <p>
     * Default CSRF configuration:
     * <ul>
     * <li>Disables CSRF protection</li>
     * </ul>
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
                .withReferrerPolicy(ReferrerPolicy.STRICT_ORIGIN)
                .withCacheControl(CacheControlPolicy.NO_CACHE)
                .withCSP()
                .withDefaultSources(ContentSecurityPolicy.SELF)
                .withStyleSources(ContentSecurityPolicy.SELF, ContentSecurityPolicy.UNSAFE_INLINE, "*")
                .withScriptSources(ContentSecurityPolicy.SELF, ContentSecurityPolicy.UNSAFE_INLINE,
                        ContentSecurityPolicy.UNSAFE_EVAL, "*")
                .withFontSources(ContentSecurityPolicy.SELF, "https://fonts.googleapis.com",
                        "https://fonts.gstatic.com", "https://cdnjs.cloudflare.com", "*")
                .withImageSources(ContentSecurityPolicy.SELF, "*")
                .withConnectSources(ContentSecurityPolicy.SELF, "*")
                .withFrameSources(ContentSecurityPolicy.SELF, "*")
                .withMediaSources(ContentSecurityPolicy.SELF, "*")
                .and()
                .httpsOnly(false);

        withCSRF()
                .disable();
        return this;
    }
}