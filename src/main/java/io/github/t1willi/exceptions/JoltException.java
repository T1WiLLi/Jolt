package io.github.t1willi.exceptions;

/**
 * General-purpose exception for errors in the Jolt framework.
 */
public class JoltException extends RuntimeException {
    /**
     * Constructs a new JoltException with a message.
     *
     * @param message The detail message explaining the exception.
     */
    public JoltException(String message) {
        super(message);
    }

    /**
     * Constructs a new JoltException with a message and cause.
     *
     * @param message The detail message explaining the exception.
     * @param cause   The underlying cause of the exception.
     */
    public JoltException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new JoltException with a cause.
     *
     * @param cause The underlying cause of the exception.
     */
    public JoltException(Throwable cause) {
        super(cause);
    }
}