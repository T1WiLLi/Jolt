package ca.jolt.exceptions;

public class BeanNotFoundException extends JoltDIException {
    public BeanNotFoundException(String message) {
        super(message);
    }
}
