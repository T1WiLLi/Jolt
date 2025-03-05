package ca.jolt.security.config;

import lombok.Getter;

public class CorsConfiguration {
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

    public CorsConfiguration allowedOrigins(String... origins) {
        this.allowedOrigins = String.join(", ", origins);
        return this;
    }

    public CorsConfiguration allowedMethods(String... methods) {
        this.allowedMethods = String.join(", ", methods);
        return this;
    }

    public CorsConfiguration allowedHeaders(String... headers) {
        this.allowedHeaders = String.join(", ", headers);
        return this;
    }

    public CorsConfiguration allowedCredentials(boolean allow) {
        this.allowCredentials = allow;
        return this;
    }

    public CorsConfiguration maxAge(long seconds) {
        this.maxAge = seconds;
        return this;
    }
}