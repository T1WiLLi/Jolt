package ca.jolt.security.config;

import lombok.Getter;

public class HeadersConfiguration {
    @Getter
    private boolean xssProtectionEnabled = true;

    @Getter
    private boolean frameOptionsEnabled = true;

    @Getter
    private boolean hstsEnabled = false;

    @Getter
    private String referrerPolicy = "same-origin";

    @Getter
    private boolean httpsOnly = false;

    @Getter
    private boolean contentSecurityPolicyEnabled = true;

    @Getter
    private String contentSecurityPolicy = "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data:; " +
            "connect-src 'self';";

    public HeadersConfiguration withXssProtection(boolean enabled) {
        this.xssProtectionEnabled = enabled;
        return this;
    }

    public HeadersConfiguration denyFrameOption(boolean enabled) {
        this.frameOptionsEnabled = enabled;
        return this;
    }

    public HeadersConfiguration httpStrictTransportSecurity(boolean enabled) {
        this.hstsEnabled = enabled;
        return this;
    }

    public HeadersConfiguration referrerPolicy(String policy) {
        this.referrerPolicy = policy;
        return this;
    }

    public HeadersConfiguration httpsOnly(boolean httpsOnly) {
        this.httpsOnly = httpsOnly;
        return this;
    }

    public HeadersConfiguration contentSecurityPolicy(boolean enabled) {
        this.contentSecurityPolicyEnabled = enabled;
        return this;
    }

    public HeadersConfiguration contentSecurityPolicy(String policy) {
        this.contentSecurityPolicy = policy;
        return this;
    }
}
