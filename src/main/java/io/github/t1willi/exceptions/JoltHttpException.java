package io.github.t1willi.exceptions;

import io.github.t1willi.http.HttpStatus;
import lombok.Getter;

/**
 * Base exception for HTTP-related errors in the Jolt framework.
 */
public class JoltHttpException extends RuntimeException {
    @Getter
    private final HttpStatus status;

    /**
     * Constructs a new JoltHttpException with a status and message.
     *
     * @param status  The HTTP status code associated with the exception.
     * @param message The detail message explaining the exception.
     */
    public JoltHttpException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Constructs a new JoltHttpException with a status, message, and cause.
     *
     * @param status  The HTTP status code associated with the exception.
     * @param message The detail message explaining the exception.
     * @param cause   The underlying cause of the exception.
     */
    public JoltHttpException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /**
     * Constructs a new JoltHttpException with a status and cause.
     *
     * @param status The HTTP status code associated with the exception.
     * @param cause  The underlying cause of the exception.
     */
    public JoltHttpException(HttpStatus status, Throwable cause) {
        super(cause);
        this.status = status;
    }

    /**
     * Constructs a new JoltHttpException with a numeric status code and message.
     *
     * @param code    The numeric HTTP status code.
     * @param message The detail message explaining the exception.
     */
    public JoltHttpException(int code, String message) {
        super(message);
        this.status = HttpStatus.fromCode(code);
    }
}