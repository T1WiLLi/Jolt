package ca.jolt.exceptions;

public class FormConversionException extends RuntimeException {

    public FormConversionException(String message) {
        super(message);
    }

    public FormConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
