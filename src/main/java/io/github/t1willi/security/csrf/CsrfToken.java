package io.github.t1willi.security.csrf;

import io.github.t1willi.core.JoltDispatcherServlet;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.routing.context.JoltContext;
import io.github.t1willi.security.config.CsrfConfiguration;
import io.github.t1willi.security.config.SecurityConfiguration;
import io.github.t1willi.security.cryptography.CryptographyUtils;
import io.github.t1willi.security.session.Session;
import jakarta.servlet.http.HttpServletRequest;

import java.util.logging.Logger;

/**
 * Utility class for managing CSRF tokens.
 * <p>
 * Provides methods to generate, rotate, store, and retrieve CSRF tokens.
 * Skips all operations if CSRF protection is disabled in the configuration.
 */
public final class CsrfToken {
    private static final Logger logger = Logger.getLogger(CsrfToken.class.getName());
    private static final String CSRF_COOKIE_NAME = "_csrf";

    private CsrfToken() {
    }

    /**
     * Generates a CSRF token if CSRF protection is enabled.
     * The token is sent to the client using the configured {@link CsrfHandler}.
     * 
     * @return The CSRF token, or null if CSRF is disabled or generation fails
     */
    public static String generate() {
        CsrfConfiguration config = JoltContainer.getInstance()
                .getBean(SecurityConfiguration.class)
                .getCsrfConfig();
        if (!config.isEnabled()) {
            logger.info(() -> "CSRF protection is disabled, skipping token generation");
            return null;
        }

        JoltContext context = JoltDispatcherServlet.getCurrentContext();
        if (context == null) {
            logger.warning("No current JoltContext available to generate CSRF token");
            return null;
        }

        CsrfHandler handler = config.getHandler();
        String token = CryptographyUtils.randomUrlSafeBase64(32);
        handler.sendToken(context, token, config);
        logger.info(() -> "CSRF token generated and sent via handler: " + token);
        return token;
    }

    /**
     * Rotates the CSRF token if CSRF protection is enabled.
     * 
     * @return The new CSRF token, or null if CSRF is disabled or rotation fails
     */
    public static String rotate() {
        CsrfConfiguration config = JoltContainer.getInstance()
                .getBean(SecurityConfiguration.class)
                .getCsrfConfig();
        if (!config.isEnabled()) {
            logger.info(() -> "CSRF protection is disabled, skipping token rotation");
            return null;
        }

        JoltContext context = JoltDispatcherServlet.getCurrentContext();
        if (context == null) {
            logger.warning("No current JoltContext available to rotate CSRF token");
            return null;
        }

        CsrfHandler handler = config.getHandler();
        String newToken = CryptographyUtils.randomUrlSafeBase64(32);
        handler.sendToken(context, newToken, config);
        logger.info(() -> "CSRF token rotated: " + newToken);
        return newToken;
    }

    /**
     * Stores the CSRF token in a secure cookie.
     * 
     * @param context The Jolt context
     * @param token   The CSRF token
     * @param config  The CSRF configuration
     */
    public static void storeInCookie(JoltContext context, String token, CsrfConfiguration config) {
        HttpServletRequest request = context.getRequest();

        context.addCookie()
                .setName(CSRF_COOKIE_NAME)
                .setValue(token)
                .path("/")
                .httpOnly(config.isHttpOnly())
                .secure(request.isSecure())
                .maxAge(getSessionLifetime())
                .sameSite("Strict")
                .build();

        logger.info(() -> "CSRF token stored in secure cookie: " + CSRF_COOKIE_NAME + " with value: " + token);
    }

    /**
     * Retrieves the CSRF token from the cookie.
     * 
     * @param context The Jolt context
     * @return The CSRF token, or null if not found
     */
    public static String getFromCookie(JoltContext context) {
        String token = context.getCookieValue(CSRF_COOKIE_NAME).orElse(null);
        logger.info(() -> "Retrieved CSRF token from cookie: " + (token != null ? token : "none"));
        return token;
    }

    /**
     * Retrieves the session lifetime from the Session class (in seconds), or uses a
     * default if no session exists.
     * 
     * @return The session lifetime in seconds
     */
    private static int getSessionLifetime() {
        try {
            if (Session.exists()) {
                Object lifetime = Session.get("session.lifetime");
                return lifetime != null ? Integer.parseInt(lifetime.toString()) : 1800; // Default 30 minutes
            }
            return 1800;
        } catch (Exception e) {
            logger.warning("Failed to retrieve session lifetime, using default: " + e.getMessage());
            return 1800;
        }
    }
}