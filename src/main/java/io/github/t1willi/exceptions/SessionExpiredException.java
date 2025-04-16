package io.github.t1willi.exceptions;

public class SessionExpiredException extends JoltSecurityException {

    public SessionExpiredException() {
        super("Session has expired");
    }

    public SessionExpiredException(String message) {
        super(message);
    }

    public SessionExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
