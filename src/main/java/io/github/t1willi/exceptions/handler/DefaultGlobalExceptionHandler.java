package io.github.t1willi.exceptions.handler;

import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.injector.annotation.JoltConfiguration;
import io.github.t1willi.injector.type.ConfigurationType;
import jakarta.servlet.http.HttpServletResponse;

import java.util.logging.Logger;

/**
 * Provides a default mechanism for handling exceptions in a Jolt-based
 * application.
 * <p>
 * Extends {@link AbstractGlobalExceptionHandler} to ensure standard
 * {@link JoltHttpException} processing is applied before this handler's logic.
 */
@JoltConfiguration(value = ConfigurationType.EXCEPTION_HANDLER, isDefault = true)
public final class DefaultGlobalExceptionHandler extends GlobalExceptionHandler {

    private static final Logger log = Logger.getLogger(DefaultGlobalExceptionHandler.class.getName());

    @Override
    public void handle(Throwable t, HttpServletResponse res) {
        System.out.println(res.getStatus());
        res.setHeader("Content-Security-Policy",
                "default-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'");

        try {
            String message = t.getMessage() != null && !t.getMessage().isBlank() ? t.getMessage()
                    : HttpStatus.fromCode(res.getStatus()).reason();
            res.sendError(res.getStatus(), message);
        } catch (Exception ex) {
            log.severe("Failed to write error message: " + ex.getMessage());
        }
    }
}