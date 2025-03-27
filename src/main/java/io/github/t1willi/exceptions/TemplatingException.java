package io.github.t1willi.exceptions;

/**
 * Represents an exception that occurs during the templating process in the Jolt
 * framework.
 * This exception is a specialized form of {@link JoltException} and is used to
 * indicate
 * issues specifically related to templating operations.
 */
public class TemplatingException extends JoltException {

    /**
     * Constructs a new {@code TemplatingException} with the specified detail
     * message.
     *
     * @param message the detail message explaining the reason for the exception
     */
    public TemplatingException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code TemplatingException} with the specified detail
     * message
     * and cause.
     *
     * @param message the detail message explaining the reason for the exception
     * @param cause   the cause of the exception (a {@code null} value is permitted,
     *                and indicates that the cause is nonexistent or unknown)
     */
    public TemplatingException(String message, Throwable cause) {
        super(message, cause);
    }
}
