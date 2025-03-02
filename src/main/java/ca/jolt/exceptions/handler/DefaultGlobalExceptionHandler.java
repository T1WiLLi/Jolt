package ca.jolt.exceptions.handler;

import java.util.logging.Logger;

import ca.jolt.exceptions.JoltHttpException;
import ca.jolt.http.HttpStatus;
import ca.jolt.injector.annotation.JoltConfiguration;
import ca.jolt.injector.type.ConfigurationType;
import jakarta.servlet.http.HttpServletResponse;

@JoltConfiguration(value = ConfigurationType.EXCEPTION_HANDLER, isDefault = true)
public final class DefaultGlobalExceptionHandler implements GlobalExceptionHandler {

    private static final Logger log = Logger.getLogger(DefaultGlobalExceptionHandler.class.getName());

    /**
     * {@inheritdoc}
     */
    @Override
    public void handle(Throwable t, HttpServletResponse res) {
        log.severe("Unhandled exception : " + t.getMessage());

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
