package io.github.t1willi.injector.type;

/**
 * Enum representing the configuration type of a bean.
 */
public enum ConfigurationType {
    /**
     * Represents a bean configured to handle exceptions.
     */
    EXCEPTION_HANDLER,

    /**
     * Represents a bean configured for security-related functionality.
     */
    SECURITY,

    /**
     * Represents a bean configured for server-related functionality.
     */
    SERVER,

    /**
     * Represents a bean configured to act as a filter.
     */
    FILTER,

    /**
     * Represents a bean configured to act as a template.
     */
    TEMPLATE,

    /**
     * Represents a bean configured to manage cookies.
     */
    COOKIE;
}