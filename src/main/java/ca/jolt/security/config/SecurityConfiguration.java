package ca.jolt.security.config;

import lombok.Getter;

public abstract class SecurityConfiguration {

    protected abstract SecurityConfiguration configure();

    @Getter
    private final CorsConfiguration corsConfig = new CorsConfiguration();

    @Getter
    private final HeadersConfiguration headersConfig = new HeadersConfiguration();

    public CorsConfiguration withCORS() {
        return corsConfig;
    }

    public HeadersConfiguration withHeaders() {
        return headersConfig;
    }
}