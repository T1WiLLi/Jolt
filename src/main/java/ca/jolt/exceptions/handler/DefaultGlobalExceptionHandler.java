package ca.jolt.exceptions.handler;

import java.util.logging.Logger;

import ca.jolt.exceptions.JoltHttpException;
import ca.jolt.http.HttpStatus;
import ca.jolt.injector.annotation.JoltConfiguration;
import ca.jolt.injector.type.ConfigurationType;
import jakarta.servlet.http.HttpServletResponse;

/**
 * The {@code DefaultGlobalExceptionHandler} provides a default mechanism
 * for handling exceptions within a Jolt-based application.
 * <p>
 * It inspects the exception, determines the appropriate HTTP status code,
 * and sends an error response using
 * {@link HttpServletResponse#sendError(int, String)}.
 * If the exception is a {@link JoltHttpException}, this handler extracts
 * the status from it; otherwise, it defaults to
 * {@link HttpStatus#INTERNAL_SERVER_ERROR}.
 * </p>
 * 
 * <p>
 * This class is annotated with {@link JoltConfiguration} to indicate it is the
 * default exception handler in the Jolt framework. Applications can create
 * their
 * own global exception handler by implementing {@link GlobalExceptionHandler}
 * and overriding the default using {@code @JoltConfiguration} with
 * {@code isDefault=false}.
 * </p>
 *
 * @author William Beaudin
 * @since 1.0
 */
@JoltConfiguration(value = ConfigurationType.EXCEPTION_HANDLER, isDefault = true)
public final class DefaultGlobalExceptionHandler implements GlobalExceptionHandler {

    private static final Logger log = Logger.getLogger(DefaultGlobalExceptionHandler.class.getName());

    /**
     * Handles the given {@code Throwable} by sending an HTTP error response.
     * <p>
     * If the exception is a {@link JoltHttpException}, the corresponding
     * {@link HttpStatus} is used; otherwise, the status defaults to
     * {@link HttpStatus#INTERNAL_SERVER_ERROR}. The message is written to
     * the response unless it is blank, in which case the
     * {@code HttpStatus} reason is used.
     * </p>
     * 
     * @param t
     *            The {@code Throwable} to handle.
     * @param res
     *            The {@link HttpServletResponse} in which the error information
     *            is written.
     */
    @Override
    public void handle(Throwable t, HttpServletResponse res) {
        log.warning("Unhandled exception : " + t.getMessage());

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
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
