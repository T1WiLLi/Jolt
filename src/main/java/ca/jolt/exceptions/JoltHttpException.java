package ca.jolt.exceptions;

import ca.jolt.http.HttpStatus;
import lombok.Getter;

public class JoltHttpException extends RuntimeException {
    @Getter
    private final HttpStatus status;

    public JoltHttpException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
