package io.github.t1willi.security.config;

import io.github.t1willi.injector.annotation.Configuration;
import io.github.t1willi.security.policies.CacheControlPolicy;
import io.github.t1willi.security.policies.ContentSecurityPolicy;
import io.github.t1willi.security.policies.FrameOptionsPolicy;
import io.github.t1willi.security.policies.HstsPolicy;
import io.github.t1willi.security.policies.ReferrerPolicy;
import io.github.t1willi.security.policies.XssProtectionPolicy;

/**
 * Default implementation of the SecurityConfiguration.
 * Provides standard security settings for a Jolt application with sensible
 * defaults.
 * This configuration includes CORS settings and security headers configuration.
 */
@Configuration(isDefault = true)
public final class DefaultSecurityConfiguration extends SecurityConfiguration {

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
        public void configure() {
                withCORS()
                                .allowedOrigins("*")
                                .allowedMethods("GET", "POST", "PUT", "DELETE")
                                .allowedHeaders("Origin", "Content-Type", "Accept", "Authorization")
                                .allowedCredentials(false)
                                .maxAge(3600);

                withHeaders()
                                .withXssProtection(XssProtectionPolicy.ENABLE_BLOCK)
                                .withFrameOptions(FrameOptionsPolicy.DENY)
                                .withHsts(HstsPolicy.NONE)
                                .withReferrerPolicy(ReferrerPolicy.STRICT_ORIGIN)
                                .withCacheControl(CacheControlPolicy.NO_CACHE)
                                .withCSP()
                                .withDefaultSources(ContentSecurityPolicy.SELF)
                                .withStyleSources(ContentSecurityPolicy.SELF, ContentSecurityPolicy.UNSAFE_INLINE, "*")
                                .withScriptSources(ContentSecurityPolicy.SELF, ContentSecurityPolicy.UNSAFE_INLINE,
                                                ContentSecurityPolicy.UNSAFE_EVAL, "*")
                                .withFontSources(ContentSecurityPolicy.SELF, ContentSecurityPolicy.GOOGLE_FONTS,
                                                "https://fonts.gstatic.com", ContentSecurityPolicy.CDNJS, "*")
                                .withImageSources(ContentSecurityPolicy.SELF, "*")
                                .withConnectSources(ContentSecurityPolicy.SELF, "*")
                                .withFrameSources(ContentSecurityPolicy.SELF, "*")
                                .withMediaSources(ContentSecurityPolicy.SELF, "*")
                                .and()
                                .httpsOnly(false);

                withCSRF()
                                .disable();
                withMaxRequest(9999);
        }
}