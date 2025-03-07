package ca.jolt.exceptions;

/**
 * Base exception for dependency injection-related errors in the Jolt framework.
 */
public class JoltDIException extends RuntimeException {
    /**
     * Constructs a new JoltDIException with a message.
     *
     * @param message The detail message explaining the exception.
     */
    public JoltDIException(String message) {
        super(message);
    }

    /**
     * Constructs a new JoltDIException with a message and cause.
     *
     * @param message The detail message explaining the exception.
     * @param cause   The underlying cause of the exception.
     */
    public JoltDIException(String message, Throwable cause) {
        super(message, cause);
    }
}
