package ca.jolt.exceptions;

public class JoltDIException extends RuntimeException {
    public JoltDIException(String message) {
        super(message);
    }

    public JoltDIException(String message, Throwable cause) {
        super(message, cause);
    }
}
