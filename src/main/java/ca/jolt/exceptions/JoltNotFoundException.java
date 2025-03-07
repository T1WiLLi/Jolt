package ca.jolt.exceptions;

import ca.jolt.http.HttpStatus;

/**
 * Exception thrown for HTTP 404 Not Found errors.
 */
public class JoltNotFoundException extends JoltHttpException {
    /**
     * Constructs a new JoltNotFoundException with a message.
     *
     * @param message The detail message explaining the exception.
     */
    public JoltNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
