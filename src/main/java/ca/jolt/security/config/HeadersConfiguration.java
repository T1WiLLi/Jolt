package ca.jolt.security.config;

import ca.jolt.security.policies.CacheControlPolicy;
import ca.jolt.security.policies.FrameOptionsPolicy;
import ca.jolt.security.policies.HstsPolicy;
import ca.jolt.security.policies.ReferrerPolicy;
import ca.jolt.security.policies.XssProtectionPolicy;
import lombok.Getter;

/**
 * Provides configuration for HTTP security headers including XSS protection,
 * frame options, HSTS, referrer policy, and Content Security Policy (CSP).
 * <p>
 * This simplified version allows you to either enable CSP using a strict
 * default configuration or supply a custom CSP string.
 */
public class HeadersConfiguration {

    // XSS Protection settings
    @Getter
    private boolean xssProtectionEnabled = true;
    @Getter
    private String xssProtectionValue = "1; mode=block";

    // Frame Options settings
    @Getter
    private boolean frameOptionsEnabled = true;
    @Getter
    private String frameOptionsValue = "DENY";

    // HSTS settings
    @Getter
    private boolean hstsEnabled = false;
    @Getter
    private String hstsValue = "max-age=31536000; includeSubDomains";

    // Referrer Policy setting
    @Getter
    private String referrerPolicy = "same-origin";

    // HTTPS-only flag
    @Getter
    private boolean httpsOnly = false;

    // Content Security Policy (CSP) settings
    @Getter
    private boolean contentSecurityPolicyEnabled = true;
    @Getter
    private String contentSecurityPolicy = "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'; connect-src 'self';";

    @Getter
    private String cacheControlDirective;

    // -------------------- XSS Protection --------------------

    /**
     * Enables or disables XSS protection.
     *
     * @param enabled true to enable, false to disable.
     * @return this HeadersConfiguration for fluent chaining.
     */
    public HeadersConfiguration withXssProtection(boolean enabled) {
        this.xssProtectionEnabled = enabled;
        return this;
    }

    /**
     * Sets XSS protection using a specific policy.
     *
     * @param policy the XSS protection policy.
     * @return this HeadersConfiguration for fluent chaining.
     */
    public HeadersConfiguration withXssProtection(XssProtectionPolicy policy) {
        this.xssProtectionEnabled = true;
        this.xssProtectionValue = policy.getValue();
        return this;
    }

    /**
     * Sets XSS protection with a report URI.
     *
     * @param reportUri the URI to which XSS reports will be sent.
     * @return this HeadersConfiguration for fluent chaining.
     */
    public HeadersConfiguration withXssProtectionReport(String reportUri) {
        this.xssProtectionEnabled = true;
        this.xssProtectionValue = XssProtectionPolicy.ENABLE_REPORT.getValue() + reportUri;
        return this;
    }

    // -------------------- Frame Options --------------------

    /**
     * Enables or disables frame options.
     *
     * @param enabled true to enable, false to disable.
     * @return this HeadersConfiguration for fluent chaining.
     */
    public HeadersConfiguration denyFrameOption(boolean enabled) {
        this.frameOptionsEnabled = enabled;
        return this;
    }

    /**
     * Sets frame options using a specific policy.
     *
     * @param policy the frame options policy.
     * @return this HeadersConfiguration for fluent chaining.
     */
    public HeadersConfiguration withFrameOptions(FrameOptionsPolicy policy) {
        this.frameOptionsEnabled = true;
        this.frameOptionsValue = policy.getValue();
        return this;
    }

    /**
     * Allows frames from a specific origin.
     *
     * @param origin the origin that is allowed to frame this content.
     * @return this HeadersConfiguration for fluent chaining.
     */
    public HeadersConfiguration allowFramesFrom(String origin) {
        this.frameOptionsEnabled = true;
        this.frameOptionsValue = FrameOptionsPolicy.ALLOW_FROM.getValue() + origin;
        return this;
    }

    // -------------------- HSTS --------------------

    /**
     * Enables or disables HTTP Strict Transport Security (HSTS).
     *
     * @param enabled true to enable, false to disable.
     * @return this HeadersConfiguration for fluent chaining.
     */
    public HeadersConfiguration httpStrictTransportSecurity(boolean enabled) {
        this.hstsEnabled = enabled;
        return this;
    }

    /**
     * Sets HSTS using a specific policy.
     *
     * @param policy the HSTS policy.
     * @return this HeadersConfiguration for fluent chaining.
     */
    public HeadersConfiguration withHsts(HstsPolicy policy) {
        this.hstsEnabled = true;
        this.hstsValue = policy.getValue();
        return this;
    }

    /**
     * Sets HSTS with a custom max-age, with options to include subdomains and
     * preload.
     *
     * @param seconds           the max-age in seconds.
     * @param includeSubDomains true to include subdomains.
     * @param preload           true to include the preload directive.
     * @return this HeadersConfiguration for fluent chaining.
     */
    public HeadersConfiguration withHstsMaxAge(long seconds, boolean includeSubDomains, boolean preload) {
        this.hstsEnabled = true;
        StringBuilder value = new StringBuilder("max-age=").append(seconds);
        if (includeSubDomains) {
            value.append("; includeSubDomains");
        }
        if (preload) {
            value.append("; preload");
        }
        this.hstsValue = value.toString();
        return this;
    }

    // -------------------- Referrer Policy --------------------

    /**
     * Sets the referrer policy using a raw string.
     *
     * @param policy the referrer policy.
     * @return this HeadersConfiguration for fluent chaining.
     */
    public HeadersConfiguration referrerPolicy(String policy) {
        this.referrerPolicy = policy;
        return this;
    }

    /**
     * Sets the referrer policy using a predefined enum.
     *
     * @param policy the predefined referrer policy.
     * @return this HeadersConfiguration for fluent chaining.
     */
    public HeadersConfiguration withReferrerPolicy(ReferrerPolicy policy) {
        this.referrerPolicy = policy.getValue();
        return this;
    }

    // -------------------- HTTPS Only --------------------

    /**
     * Enables or disables HTTPS-only mode.
     *
     * @param httpsOnly true to enforce HTTPS, false to allow HTTP.
     * @return this HeadersConfiguration for fluent chaining.
     */
    public HeadersConfiguration httpsOnly(boolean httpsOnly) {
        this.httpsOnly = httpsOnly;
        return this;
    }

    // -------------------- Content Security Policy (CSP) --------------------

    /**
     * Enables or disables Content Security Policy (CSP) using a strict default.
     * When enabled, the default strict CSP is:
     * "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self';
     * connect-src 'self';"
     *
     * @param enabled true to enable CSP using the default strict configuration.
     * @return this HeadersConfiguration for fluent chaining.
     */
    public HeadersConfiguration contentSecurityPolicy(boolean enabled) {
        this.contentSecurityPolicyEnabled = enabled;
        return this;
    }

    /**
     * Sets a custom Content Security Policy.
     *
     * @param policy a custom CSP string.
     * @return this HeadersConfiguration for fluent chaining.
     */
    public HeadersConfiguration contentSecurityPolicy(String policy) {
        this.contentSecurityPolicyEnabled = true;
        this.contentSecurityPolicy = policy;
        return this;
    }

    /**
     * Sets the Cache-Control header using a predefined policy.
     *
     * @param policy the CacheControlPolicy to apply
     * @return this HeadersConfiguration for fluent chaining.
     */
    public HeadersConfiguration withCacheControl(CacheControlPolicy policy) {
        this.cacheControlDirective = policy.getValue();
        return this;
    }

    // Optionally, you could also provide a raw setter if needed:
    public HeadersConfiguration withCacheControl(String cacheDirectives) {
        this.cacheControlDirective = cacheDirectives;
        return this;
    }
}