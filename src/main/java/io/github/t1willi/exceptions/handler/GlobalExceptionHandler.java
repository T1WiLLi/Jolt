package io.github.t1willi.exceptions.handler;

import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.http.HttpStatus;
import jakarta.servlet.http.HttpServletResponse;

public abstract class GlobalExceptionHandler implements ExceptionHandler {
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
        }
        response.setStatus(status.code());
        handle(t, response);
    }
}
