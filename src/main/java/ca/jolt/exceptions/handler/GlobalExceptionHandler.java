package ca.jolt.exceptions.handler;

import ca.jolt.routing.context.JoltHttpContext;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Defines an interface for handling unhandled exceptions in Jolt.
 * <p>
 * Implementations may provide custom exception handling logic, or rely on
 * {@link DefaultGlobalExceptionHandler} as a default.
 */
public interface GlobalExceptionHandler {

    /**
     * Handles a thrown exception using the provided {@link JoltHttpContext}.
     * <p>
     * By default, this method delegates to
     * {@link #handle(Throwable, HttpServletResponse)},
     * passing {@code ctx.getResponse()}.
     *
     * @param t   The thrown exception
     * @param ctx The current request and response context
     */
    default void handle(Throwable t, JoltHttpContext ctx) {
        handle(t, ctx.getResponse());
    }

    /**
     * Handles a thrown exception using the provided {@link HttpServletResponse}.
     *
     * @param t        The thrown exception
     * @param response The HTTP response for the current request
     */
    void handle(Throwable t, HttpServletResponse response);
}
