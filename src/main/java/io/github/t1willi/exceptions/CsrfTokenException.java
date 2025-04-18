package io.github.t1willi.exceptions;

import io.github.t1willi.http.HttpStatus;

public class CsrfTokenException extends JoltHttpException {

    public CsrfTokenException(HttpStatus status, String message) {
        super(status, message);
    }

    public CsrfTokenException(HttpStatus status, String message, Throwable cause) {
        super(status, message, cause);
    }
}
