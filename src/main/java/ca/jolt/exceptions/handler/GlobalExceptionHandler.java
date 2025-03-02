package ca.jolt.exceptions.handler;

import ca.jolt.routing.context.JoltHttpContext;
import jakarta.servlet.http.HttpServletResponse;

/**
 * A simple interface for handling unhandled exceptions in Jolt.
 * Users can implement their own logic, or rely on
 * {@link DefaultGlobalExceptionHandler}.
 */
public interface GlobalExceptionHandler {

    /**
     * Called whenever a route handler or the framework throws an exception that
     * isn't handled more specifically.
     *
     * @param t   the Throwable that was caught
     * @param ctx the JoltHttpContext for the request/response
     */
    default void handle(Throwable t, JoltHttpContext ctx) {
        handle(t, ctx.getResponse());
    }

    /**
     * Called whenever a route handler or the framework throws an exception that
     * isn't handled more specifically.
     *
     * @param t        the Throwable that was caught
     * @param response the HttpsServletResponse for the request
     */
    void handle(Throwable t, HttpServletResponse response);
}