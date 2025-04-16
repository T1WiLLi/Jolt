package io.github.t1willi.exceptions;

public class SessionUserAgentMismatchException extends JoltSecurityException {
    public SessionUserAgentMismatchException() {
        super("Session User Agent Mismatch");
    }

    public SessionUserAgentMismatchException(String message) {
        super(message);
    }

    public SessionUserAgentMismatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
