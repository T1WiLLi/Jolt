package ca.jolt.exceptions;

/**
 * Exception thrown when the body of a request or response cannot be built.
 */
public class FailedToBuildBodyException extends RuntimeException {
    /**
     * Constructs a new FailedToBuildBodyException with a message.
     *
     * @param message The detail message explaining the exception.
     */
    public FailedToBuildBodyException(String message) {
        super(message);
    }

    /**
     * Constructs a new FailedToBuildBodyException with a message and cause.
     *
     * @param message The detail message explaining the exception.
     * @param cause   The underlying cause of the exception.
     */
    public FailedToBuildBodyException(String message, Throwable cause) {
        super(message, cause);
    }
}
