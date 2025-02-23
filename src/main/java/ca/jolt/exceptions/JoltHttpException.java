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

    public JoltHttpException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public JoltHttpException(HttpStatus status, Throwable cause) {
        super(cause);
        this.status = status;
    }

    public JoltHttpException(int code, String message) {
        super(message);
        this.status = HttpStatus.fromCode(code);
    }
}
