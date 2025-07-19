package io.github.t1willi.exceptions;

import io.github.t1willi.http.HttpStatus;

public class SessionUserAgentMismatchException extends JoltHttpException {
    public SessionUserAgentMismatchException() {
        super(HttpStatus.FORBIDDEN, "Session User Agent Mismatch");
    }

    public SessionUserAgentMismatchException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }

    public SessionUserAgentMismatchException(String message, Throwable cause) {
        super(HttpStatus.FORBIDDEN, message, cause);
    }
}
