package ca.jolt.exceptions;

public class FailedToBuildBodyException extends RuntimeException {
    public FailedToBuildBodyException(String message) {
        super(message);
    }

    public FailedToBuildBodyException(String message, Throwable cause) {
        super(message, cause);
    }
}
