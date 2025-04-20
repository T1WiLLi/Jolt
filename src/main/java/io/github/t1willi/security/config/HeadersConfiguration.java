package io.github.t1willi.security.config;

import io.github.t1willi.security.policies.CacheControlPolicy;
import io.github.t1willi.security.policies.ContentSecurityPolicy;
import io.github.t1willi.security.policies.FrameOptionsPolicy;
import io.github.t1willi.security.policies.HstsPolicy;
import io.github.t1willi.security.policies.ReferrerPolicy;
import io.github.t1willi.security.policies.XssProtectionPolicy;
import lombok.Getter;

/**
 * Provides configuration for HTTP security headers including XSS protection,
 * frame options, HSTS, referrer policy, and Content Security Policy (CSP).
 */
public class HeadersConfiguration {

    private final SecurityConfiguration parent;

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
    private boolean hstsEnabled = true;
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
    private String contentSecurityPolicy; // No default value; rely on builder
    private ContentSecurityPolicy cspBuilder = new ContentSecurityPolicy(this);

    @Getter
    private String cacheControlDirective;

    public HeadersConfiguration(SecurityConfiguration parent) {
        this.parent = parent;
    }

    public SecurityConfiguration and() {
        return parent;
    }

    // -------------------- XSS Protection --------------------

    public HeadersConfiguration withXssProtection(boolean enabled) {
        this.xssProtectionEnabled = enabled;
        return this;
    }

    public HeadersConfiguration withXssProtection(XssProtectionPolicy policy) {
        this.xssProtectionEnabled = true;
        this.xssProtectionValue = policy.getValue();
        return this;
    }

    public HeadersConfiguration withXssProtectionReport(String reportUri) {
        this.xssProtectionEnabled = true;
        this.xssProtectionValue = XssProtectionPolicy.ENABLE_REPORT.getValue() + reportUri;
        return this;
    }

    // -------------------- Frame Options --------------------

    public HeadersConfiguration denyFrameOption(boolean enabled) {
        this.frameOptionsEnabled = enabled;
        return this;
    }

    public HeadersConfiguration withFrameOptions(FrameOptionsPolicy policy) {
        this.frameOptionsEnabled = true;
        this.frameOptionsValue = policy.getValue();
        return this;
    }

    public HeadersConfiguration allowFramesFrom(String origin) {
        this.frameOptionsEnabled = true;
        this.frameOptionsValue = FrameOptionsPolicy.ALLOW_FROM.getValue() + origin;
        return this;
    }

    // -------------------- HSTS --------------------

    public HeadersConfiguration httpStrictTransportSecurity(boolean enabled) {
        this.hstsEnabled = enabled;
        return this;
    }

    public HeadersConfiguration withHsts(HstsPolicy policy) {
        this.hstsEnabled = true;
        this.hstsValue = policy.getValue();
        return this;
    }

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

    public HeadersConfiguration referrerPolicy(String policy) {
        this.referrerPolicy = policy;
        return this;
    }

    public HeadersConfiguration withReferrerPolicy(ReferrerPolicy policy) {
        this.referrerPolicy = policy.getValue();
        return this;
    }

    // -------------------- HTTPS Only --------------------

    public HeadersConfiguration httpsOnly(boolean httpsOnly) {
        this.httpsOnly = httpsOnly;
        return this;
    }

    // -------------------- Content Security Policy (CSP) --------------------

    public ContentSecurityPolicy withCSP() {
        this.contentSecurityPolicyEnabled = true;
        return cspBuilder;
    }

    public HeadersConfiguration contentSecurityPolicy(String policy) {
        this.contentSecurityPolicyEnabled = true;
        this.contentSecurityPolicy = policy;
        this.cspBuilder = null;
        return this;
    }

    public String getContentSecurityPolicy() {
        if (!contentSecurityPolicyEnabled) {
            return null;
        }
        if (cspBuilder != null) {
            return cspBuilder.build();
        }
        if (contentSecurityPolicy != null) {
            return contentSecurityPolicy;
        }
        return new ContentSecurityPolicy(this).build();
    }

    // -------------------- Cache Control --------------------

    public HeadersConfiguration withCacheControl(CacheControlPolicy policy) {
        this.cacheControlDirective = policy.getValue();
        return this;
    }

    public HeadersConfiguration withCacheControl(String cacheDirectives) {
        this.cacheControlDirective = cacheDirectives;
        return this;
    }
}