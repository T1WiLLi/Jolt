package io.github.t1willi.exceptions;

/**
 * Exception thrown when a bean cannot be created in the dependency injection
 * system.
 */
public class BeanCreationException extends JoltDIException {
    /**
     * Constructs a new BeanCreationException with a message and cause.
     *
     * @param message The detail message explaining the exception.
     * @param cause   The underlying cause of the exception.
     */
    public BeanCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
