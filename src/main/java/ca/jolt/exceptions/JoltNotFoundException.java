package ca.jolt.exceptions;

import ca.jolt.http.HttpStatus;

public class JoltNotFoundException extends JoltHttpException {
    public JoltNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
