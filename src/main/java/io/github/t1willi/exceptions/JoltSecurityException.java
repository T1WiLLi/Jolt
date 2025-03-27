package io.github.t1willi.exceptions;

/**
 * Thrown to indicate that a security violation or error has occurred within the
 * application.
 * <p>
 * This runtime exception signals security-related issues such as unauthorized
 * access or
 * failure to adhere to security policies.
 * </p>
 */
public class JoltSecurityException extends RuntimeException {

    /**
     * Constructs a new JoltSecurityException with the specified detail message.
     *
     * @param message the detail message explaining the reason for the exception.
     */
    public JoltSecurityException(String message) {
        super(message);
    }

    /**
     * Constructs a new JoltSecurityException with the specified detail message and
     * cause.
     *
     * @param message the detail message explaining the reason for the exception.
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).
     */
    public JoltSecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new JoltSecurityException with the specified cause.
     * The detail message is set to {@code (cause==null ? null : cause.toString())}.
     *
     * @param cause the cause (which is saved for later retrieval by the
     *              {@link #getCause()} method).
     */
    public JoltSecurityException(Throwable cause) {
        super(cause);
    }
}
