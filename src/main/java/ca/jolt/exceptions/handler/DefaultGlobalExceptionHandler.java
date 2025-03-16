package ca.jolt.exceptions.handler;

import java.util.logging.Logger;

import ca.jolt.exceptions.JoltHttpException;
import ca.jolt.http.HttpStatus;
import ca.jolt.injector.annotation.JoltConfiguration;
import ca.jolt.injector.type.ConfigurationType;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Provides a default mechanism for handling exceptions in a Jolt-based
 * application.
 * <p>
 * This handler inspects the thrown exception, determines an appropriate
 * HTTP status code, and sends an error response using
 * {@link HttpServletResponse#sendError(int, String)}. If the exception is a
 * {@link JoltHttpException}, its {@link HttpStatus} is used; otherwise, it
 * defaults to {@link HttpStatus#INTERNAL_SERVER_ERROR}.
 * <p>
 * Annotated with {@link JoltConfiguration} as the default exception handler.
 * Applications can define their own global exception handler by implementing
 * {@link GlobalExceptionHandler} and overriding the default via
 * {@code @JoltConfiguration(value = ConfigurationType.EXCEPTION_HANDLER)}.
 * 
 * @author William
 * @since 1.0
 */
@JoltConfiguration(value = ConfigurationType.EXCEPTION_HANDLER, isDefault = true)
public final class DefaultGlobalExceptionHandler implements GlobalExceptionHandler {

    /**
     * The logger for this exception handler.
     */
    private static final Logger log = Logger.getLogger(DefaultGlobalExceptionHandler.class.getName());

    /**
     * Handles a thrown {@link Throwable} by sending an HTTP error response.
     * <p>
     * If the exception is a {@link JoltHttpException}, the corresponding
     * {@link HttpStatus} is used. Otherwise, the status defaults to
     * {@link HttpStatus#INTERNAL_SERVER_ERROR}. The exception's message is
     * included in the response unless it is blank, in which case the
     * {@code HttpStatus} reason is used.
     *
     * @param t   The thrown {@link Throwable} to handle
     * @param res The {@link HttpServletResponse} in which the error is written
     */
    @Override
    public void handle(Throwable t, HttpServletResponse res) {
        HttpStatus status = HttpStatus.fromCode(res.getStatus());
        String message = t.getMessage();

        if (t instanceof JoltHttpException jhe) {
            status = jhe.getStatus();
            if (message == null || message.isBlank()) {
                message = status.reason();
            }
        }

        try {
            res.sendError(status.code(), message);
        } catch (Exception ex) {
            log.severe("Failed to write error message: " + ex.getMessage());
        }
    }
}
