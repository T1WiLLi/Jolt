package ca.jolt.exceptions;

public class CircularDependencyException extends JoltDIException {
    public CircularDependencyException(String message) {
        super(message);
    }
}
