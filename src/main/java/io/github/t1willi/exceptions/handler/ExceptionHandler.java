package io.github.t1willi.exceptions.handler;

import java.util.function.BiConsumer;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.exceptions.JoltHttpException;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Defines an interface for handling unhandled exceptions in Jolt.
 * <p>
 * Implementations may provide custom exception handling logic, or rely on
 * {@link DefaultGlobalExceptionHandler} as a default.
 */
public interface ExceptionHandler {

    /**
     * Handles a thrown exception using the provided {@link JoltContext}.
     * <p>
     * By default, this method delegates to
     * {@link #handle(Throwable, HttpServletResponse)},
     * passing {@code ctx.getResponse()}.
     *
     * @param t   The thrown exception
     * @param ctx The current request and response context
     */
    default void handle(Throwable t, JoltContext ctx) {
        handle(t, ctx.getResponse());
    }

    /**
     * Handles a thrown exception using the provided {@link HttpServletResponse}.
     *
     * @param t        The thrown exception
     * @param response The HTTP response for the current request
     */
    void handle(Throwable t, HttpServletResponse response);

    /**
     * Internal method to ensure standardized exception handling.
     * <p>
     * Implementations should not override this method directly; instead, they
     * should provide custom logic in
     * {@link #handle(Throwable, HttpServletResponse)}.
     * This method is called by the framework to ensure consistent processing of
     * exceptions, such as {@link JoltHttpException}.
     *
     * @param t        The thrown exception
     * @param response The HTTP response for the current request
     */
    default void handleException(Throwable t, HttpServletResponse response) {
        handle(t, response);
    }

    /**
     * Registers a handler for a specific exception type.
     * 
     * @param <T>           The type of exception to handle
     * @param exceptionType The exception type to handle
     * @param handler       The handler to use for the exception type
     */
    default <T extends Throwable> void registerHandler(Class<T> exceptionType,
            BiConsumer<T, HttpServletResponse> handler) {
        getRegistry().registerHandler(exceptionType, handler);
    }

    ExceptionHandlerRegistry getRegistry();
}
