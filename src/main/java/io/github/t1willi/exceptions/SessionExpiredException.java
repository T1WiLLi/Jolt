package io.github.t1willi.exceptions;

import io.github.t1willi.http.HttpStatus;

public class SessionExpiredException extends JoltHttpException {

    public SessionExpiredException() {
        super(HttpStatus.UNAUTHORIZED, "Session has expired");
    }

    public SessionExpiredException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

    public SessionExpiredException(String message, Throwable cause) {
        super(HttpStatus.UNAUTHORIZED, message, cause);
    }
}
