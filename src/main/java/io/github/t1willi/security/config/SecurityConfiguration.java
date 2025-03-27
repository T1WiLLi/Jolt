package io.github.t1willi.security.config;

import lombok.Getter;

/**
 * Abstract base class for security configurations in Jolt applications.
 * Provides the foundation for configuring security-related settings such as
 * CORS (Cross-Origin Resource Sharing) and HTTP security headers.
 */
public abstract class SecurityConfiguration {

    /**
     * Configures the security settings.
     * Implementing classes should define security policies in this method.
     *
     * @return This configuration instance for method chaining
     */
    protected abstract SecurityConfiguration configure();

    /**
     * The CORS configuration for this security setup.
     * Initialized with default values.
     */
    @Getter
    private final CorsConfiguration corsConfig = new CorsConfiguration();

    /**
     * The HTTP headers configuration for this security setup.
     * Initialized with default values.
     */
    @Getter
    private final HeadersConfiguration headersConfig = new HeadersConfiguration();

    /**
     * Provides access to the CORS configuration for fluent configuration.
     *
     * @return The CORS configuration instance for method chaining
     */
    public CorsConfiguration withCORS() {
        return corsConfig;
    }

    /**
     * Provides access to the headers configuration for fluent configuration.
     *
     * @return The headers configuration instance for method chaining
     */
    public HeadersConfiguration withHeaders() {
        return headersConfig;
    }
}