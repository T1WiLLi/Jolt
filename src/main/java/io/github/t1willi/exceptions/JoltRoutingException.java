package io.github.t1willi.exceptions;

/**
 * Exception thrown when a routing error occurs in the Jolt framework.
 */
public class JoltRoutingException extends RuntimeException {
    /**
     * Constructs a new JoltRoutingException with a message.
     *
     * @param message The detail message explaining the exception.
     */
    public JoltRoutingException(String message) {
        super(message);
    }
}
