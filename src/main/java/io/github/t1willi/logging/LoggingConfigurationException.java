package io.github.t1willi.logging;

/**
 * Exception thrown when the Jolt logging system fails to initialize or
 * configure properly.
 * 
 * @since 1.0
 */
public class LoggingConfigurationException extends RuntimeException {

    /**
     * Creates a new logging configuration exception.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public LoggingConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
