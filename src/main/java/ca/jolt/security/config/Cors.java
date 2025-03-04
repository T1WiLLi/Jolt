package ca.jolt.security.config;

public interface Cors {

    String getAllowedOrigins();

    String getAllowedMethods();

    String getAllowedHeaders();

    String getExposedHeaders();

    boolean allowCredentials();

    long getMaxAge();
}
