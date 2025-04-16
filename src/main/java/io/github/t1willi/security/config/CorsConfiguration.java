package io.github.t1willi.security.config;

import lombok.Getter;

/**
 * Configuration class for Cross-Origin Resource Sharing (CORS) settings.
 * Provides methods to configure various CORS parameters for HTTP requests.
 */
public class CorsConfiguration {

    private final SecurityConfiguration parent;

    @Getter
    private String allowedOrigins = "*";
    @Getter
    private String allowedMethods = "GET, POST, PUT, DELETE, OPTIONS";
    @Getter
    private String allowedHeaders = "Origin, Content-Type, Accept, Authorization";
    @Getter
    private boolean allowCredentials = false;
    @Getter
    private long maxAge = 3600;

    public CorsConfiguration(SecurityConfiguration parent) {
        this.parent = parent;
    }

    public SecurityConfiguration and() {
        return parent;
    }

    /**
     * Sets the allowed origins for CORS requests.
     *
     * @param origins Array of allowed origin domains
     * @return This configuration instance for method chaining
     */
    public CorsConfiguration allowedOrigins(String... origins) {
        this.allowedOrigins = String.join(", ", origins);
        return this;
    }

    /**
     * Sets the allowed HTTP methods for CORS requests.
     *
     * @param methods Array of allowed HTTP methods
     * @return This configuration instance for method chaining
     */
    public CorsConfiguration allowedMethods(String... methods) {
        this.allowedMethods = String.join(", ", methods);
        return this;
    }

    /**
     * Sets the allowed HTTP headers for CORS requests.
     *
     * @param headers Array of allowed HTTP headers
     * @return This configuration instance for method chaining
     */
    public CorsConfiguration allowedHeaders(String... headers) {
        this.allowedHeaders = String.join(", ", headers);
        return this;
    }

    /**
     * Sets whether credentials are allowed in CORS requests.
     *
     * @param allow True to allow credentials, false otherwise
     * @return This configuration instance for method chaining
     */
    public CorsConfiguration allowedCredentials(boolean allow) {
        this.allowCredentials = allow;
        return this;
    }

    /**
     * Sets the maximum age (in seconds) of the cache duration for preflight
     * responses.
     *
     * @param seconds Cache duration in seconds
     * @return This configuration instance for method chaining
     */
    public CorsConfiguration maxAge(long seconds) {
        this.maxAge = seconds;
        return this;
    }
}