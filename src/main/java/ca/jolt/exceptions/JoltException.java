package ca.jolt.exceptions;

public class JoltException extends RuntimeException {
    public JoltException(String message) {
        super(message);
    }

    public JoltException(String message, Throwable cause) {
        super(message, cause);
    }

    public JoltException(Throwable cause) {
        super(cause);
    }
}
