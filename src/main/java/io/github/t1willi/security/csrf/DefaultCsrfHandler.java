package io.github.t1willi.security.csrf;

import java.util.logging.Logger;

import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.routing.context.JoltContext;
import io.github.t1willi.security.config.CsrfConfiguration;

/**
 * Default implementation of {@link CsrfHandler} that uses secure cookies for
 * token storage.
 * <p>
 * This handler:
 * <ul>
 * <li>Stores the CSRF token in a secure cookie using
 * {@link CsrfToken#storeInCookie}.</li>
 * <li>Retrieves the token from the cookie using
 * {@link CsrfToken#getFromCookie}.</li>
 * <li>Expects the token in either the "X-CSRF-TOKEN" header or a form parameter
 * (name configurable via {@link CsrfConfiguration}).</li>
 * <li>Validates the token for modifying HTTP methods (POST, PUT, DELETE,
 * PATCH).</li>
 * <li>Rotates the token after successful validation to prevent reuse.</li>
 * </ul>
 */
public final class DefaultCsrfHandler implements CsrfHandler {
    private static final Logger logger = Logger.getLogger(DefaultCsrfHandler.class.getName());
    private static final String CSRF_HEADER = "X-CSRF-TOKEN";

    /**
     * Validates the CSRF token for the given request.
     * - Skips validation if CSRF is disabled in the configuration.
     * - Skips validation for ignored URL patterns.
     * - Skips validation for non-modifying HTTP methods (GET, HEAD, OPTIONS).
     * - Compares the stored token (from the cookie) with the token provided in the
     * request
     * (via header or form parameter)
     * 
     * @param context The Jolt context
     * @param config  The CSRF configuration
     * @throws JoltHttpException if validation fails
     */
    @Override
    public void validate(JoltContext context, CsrfConfiguration config) throws JoltHttpException {
        if (!config.isEnabled()) {
            logger.fine("CSRF protection is disabled, skipping validation");
            return;
        }

        String path = context.requestPath();
        if (config.getIgnoreUrlPatterns().stream().anyMatch(path::startsWith)) {
            logger.fine(() -> "Path " + path + " is ignored for CSRF validation");
            return;
        }

        String method = context.method().toUpperCase();
        if (!method.equals("POST") && !method.equals("PUT") && !method.equals("DELETE") && !method.equals("PATCH")) {
            logger.fine(() -> "Method " + method + " does not require CSRF validation");
            return;
        }

        String storedToken = receiveToken(context, config);
        if (storedToken == null) {
            logger.warning("CSRF token missing in storage (cookie)");
            throw new JoltHttpException(HttpStatus.FORBIDDEN,
                    "CSRF token missing in storage. Ensure the token is generated and stored correctly.");
        }

        String expectedToken = storedToken;
        logger.fine(() -> "Expected CSRF token: " + expectedToken);

        String requestToken = context.getHeader(CSRF_HEADER);
        if (requestToken == null) {
            requestToken = context.query(config.getTokenName());
        }

        if (requestToken == null) {
            logger.warning("CSRF token missing in request (header or form parameter)");
            throw new JoltHttpException(HttpStatus.FORBIDDEN,
                    "CSRF token missing in request. Ensure the token is sent in the 'X-CSRF-TOKEN' header or as a form parameter named '"
                            + config.getTokenName() + "'.");
        }

        if (!expectedToken.equals(requestToken)) {
            logger.warning("CSRF token mismatch: expected " + expectedToken + ", received " + requestToken);
            throw new JoltHttpException(HttpStatus.FORBIDDEN,
                    "Invalid CSRF token. The provided token does not match the expected token.");
        }

        CsrfToken.rotate();
        logger.info(() -> "CSRF token validated successfully for path: " + path + " with method: " + method);
    }

    /**
     * Sends the CSRF token to the client by storing it in a secure cookie.
     * 
     * @param context The Jolt context
     * @param token   The CSRF token
     * @param config  The CSRF configuration
     */
    @Override
    public void sendToken(JoltContext context, String token, CsrfConfiguration config) {
        CsrfToken.storeInCookie(context, token, config);
    }

    /**
     * Receives the CSRF token from the client by retrieving it from the cookie.
     * 
     * @param context The Jolt context
     * @param config  The CSRF configuration
     * @return The received CSRF token, or null if not found
     */
    @Override
    public String receiveToken(JoltContext context, CsrfConfiguration config) {
        return CsrfToken.getFromCookie(context);
    }
}
