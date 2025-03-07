package ca.jolt.exceptions;

/**
 * Exception thrown when a circular dependency is detected in the dependency
 * injection system.
 */
public class CircularDependencyException extends JoltDIException {
    /**
     * Constructs a new CircularDependencyException with a message.
     *
     * @param message The detail message explaining the exception.
     */
    public CircularDependencyException(String message) {
        super(message);
    }
}
