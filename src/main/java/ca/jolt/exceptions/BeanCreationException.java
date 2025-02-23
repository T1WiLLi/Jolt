package ca.jolt.exceptions;

public class BeanCreationException extends JoltDIException {
    public BeanCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
