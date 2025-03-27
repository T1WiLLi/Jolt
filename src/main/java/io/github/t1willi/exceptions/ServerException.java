package io.github.t1willi.exceptions;

/**
 * Exception thrown for general server-side errors.
 */
public class ServerException extends Exception {
    /**
     * Constructs a new ServerException with a message.
     *
     * @param message The detail message explaining the exception.
     */
    public ServerException(String message) {
        super(message);
    }

    /**
     * Constructs a new ServerException with a message and cause.
     *
     * @param message The detail message explaining the exception.
     * @param cause   The underlying cause of the exception.
     */
    public ServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
