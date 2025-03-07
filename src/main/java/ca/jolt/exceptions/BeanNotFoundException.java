package ca.jolt.exceptions;

/**
 * Exception thrown when a requested bean is not found in the dependency
 * injection system.
 */
public class BeanNotFoundException extends JoltDIException {
    /**
     * Constructs a new BeanNotFoundException with a message.
     *
     * @param message The detail message explaining the exception.
     */
    public BeanNotFoundException(String message) {
        super(message);
    }
}
