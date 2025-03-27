package io.github.t1willi.exceptions;

/**
 * Exception thrown when a form value cannot be converted to the expected type.
 */
public class FormConversionException extends RuntimeException {
    /**
     * Constructs a new FormConversionException with a message.
     *
     * @param message The detail message explaining the exception.
     */
    public FormConversionException(String message) {
        super(message);
    }

    /**
     * Constructs a new FormConversionException with a message and cause.
     *
     * @param message The detail message explaining the exception.
     * @param cause   The underlying cause of the exception.
     */
    public FormConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
