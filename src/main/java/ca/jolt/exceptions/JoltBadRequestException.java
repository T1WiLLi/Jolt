package ca.jolt.exceptions;

import ca.jolt.http.HttpStatus;

public class JoltBadRequestException extends JoltHttpException {
    public JoltBadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
