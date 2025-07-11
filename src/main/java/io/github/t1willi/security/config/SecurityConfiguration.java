package io.github.t1willi.security.config;

import io.github.t1willi.utils.Constant;
import lombok.Getter;

/**
 * Abstract base class for security configurations in Jolt applications.
 * Provides the foundation for configuring security-related settings such as
 * CORS (Cross-Origin Resource Sharing) and HTTP security headers.
 */
public abstract class SecurityConfiguration {

    /**
     * Amount of request a given user can make in a given time frame. (1 sec).
     * This is used to prevent brute force attacks.
     */
    @Getter
    private int maxRequest = Constant.Security.DEFAULT_MAX_REQUEST_PER_USER_PER_SECOND;

    /**
     * Configures the security settings.
     * Implementing classes should define security policies in this method.
     *
     * @return This configuration instance for method chaining
     */
    protected abstract void configure();

    /**
     * The CORS configuration for this security setup.
     * Initialized with default values.
     */
    @Getter
    private final CorsConfiguration corsConfig = new CorsConfiguration(this);

    /**
     * The HTTP headers configuration for this security setup.
     * Initialized with default values.
     */
    @Getter
    private final HeadersConfiguration headersConfig = new HeadersConfiguration(this);

    /**
     * The CSRF configuration for this security setup. Initialized with default
     * values.
     */
    @Getter
    private final CsrfConfiguration csrfConfig = new CsrfConfiguration(this);

    /**
     * The route configuration for this security setup. Initialized with default
     * values.
     */
    @Getter
    private final RouteConfiguration routeConfig = new RouteConfiguration(this);

    /**
     * The Nonce configuration for this security setup. Initialized with default
     * values.
     */
    @Getter
    private final NonceConfiguration nonceConfig = new NonceConfiguration(this);

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

    /**
     * Start route-based authentication configuration.
     */
    public RouteConfiguration withRoutes() {
        return routeConfig;
    }

    /**
     * Provides access to the CSRF configuration for fluent configuration.
     * 
     * @return The CSRF configuration instance for method chaining
     */
    public CsrfConfiguration withCSRF() {
        return csrfConfig;
    }

    /**
     * Provides access to the nonce configuration for fluent configuration.
     * 
     * @return The nonce configuration instance for method chaining
     */
    public NonceConfiguration withNonce() {
        return nonceConfig;
    }

    /**
     * Provides access to the maximum request rate configuration for fluent
     * configuration.
     * 
     * @param maxRequest the maximum request rate per user per second
     * @return This configuration instance for method chaining
     */
    public SecurityConfiguration withMaxRequest(int maxRequest) {
        this.maxRequest = maxRequest;
        return this;
    }

    protected SecurityConfiguration() {
        configure();
    }
}