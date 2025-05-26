package io.github.t1willi.exceptions.handler;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.core.JoltDispatcherServlet;
import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.http.HttpStatus;
import jakarta.servlet.http.HttpServletResponse;

public abstract class GlobalExceptionHandler implements ExceptionHandler {
    private final ExceptionHandlerRegistry registry = new ExceptionHandlerRegistry();

    public void init() {
        registry.registerAnnotatedHandler(this);
    }

    @Override
    public ExceptionHandlerRegistry getRegistry() {
        return this.registry;
    }

    /**
     * Processes the exception with standard {@link JoltHttpException} handling
     * and delegates to {@link #handle(Throwable, HttpServletResponse)}.
     * <p>
     * This method is final to ensure consistent exception processing. Custom
     * handlers should implement {@link #handle(Throwable, HttpServletResponse)}.
     *
     * @param t        The thrown exception
     * @param response The HTTP response for the current request
     */
    @Override
    public final void handleException(Throwable t, HttpServletResponse response) {
        HttpStatus status = HttpStatus.fromCode(response.getStatus());
        String message = t.getMessage();

        if (t instanceof JoltHttpException jhe) {
            status = jhe.getStatus();
            if (message == null || message.isBlank()) {
                message = status.reason();
            }
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = message != null && !message.isBlank() ? message : status.reason();
        }

        if (response.getStatus() == HttpStatus.OK.code()) {
            response.setStatus(status.code());
        }

        if (!this.registry.handleSpecificException(t, response)) {
            this.handle(t, response); // fallback to default handling
        }
    }

    /**
     * This method is called to retrieve the current JoltContext object for this
     * request.
     * Allowing the user to make different responses and not just default to the
     * HttpServletResponse.
     * 
     * @return The current JoltContext object for this request.
     */
    public final JoltContext context() {
        return JoltDispatcherServlet.getCurrentContext();
    }
}
