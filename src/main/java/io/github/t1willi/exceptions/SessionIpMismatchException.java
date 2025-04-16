package io.github.t1willi.exceptions;

public class SessionIpMismatchException extends JoltSecurityException {
    public SessionIpMismatchException() {
        super("Session IP mismatch");
    }

    public SessionIpMismatchException(String message) {
        super(message);
    }

    public SessionIpMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
