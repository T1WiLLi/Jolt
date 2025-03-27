package io.github.t1willi.exceptions;

/**
 * Exception thrown when a duplicate route is detected in the routing system.
 */
public class DuplicateRouteException extends RuntimeException {
    /**
     * Constructs a new DuplicateRouteException with a message.
     *
     * @param message The detail message explaining the exception.
     */
    public DuplicateRouteException(String message) {
        super(message);
    }
}
