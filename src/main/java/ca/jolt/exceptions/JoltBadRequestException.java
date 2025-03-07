package ca.jolt.exceptions;

import ca.jolt.http.HttpStatus;

/**
 * Exception thrown for HTTP 400 Bad Request errors.
 */
public class JoltBadRequestException extends JoltHttpException {
    /**
     * Constructs a new JoltBadRequestException with a message.
     *
     * @param message The detail message explaining the exception.
     */
    public JoltBadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
