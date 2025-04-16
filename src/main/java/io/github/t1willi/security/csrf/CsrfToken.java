package io.github.t1willi.security.csrf;

import java.util.logging.Logger;

import io.github.t1willi.core.JoltDispatcherServlet;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.routing.context.JoltContext;
import io.github.t1willi.security.config.CsrfConfiguration;
import io.github.t1willi.security.config.SecurityConfiguration;
import io.github.t1willi.security.cryptography.CryptographyUtils;
import io.github.t1willi.security.session.Session;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for managing CSRF tokens.
 * <p>
 * Provides methods to generate, rotate, store, and retrieve CSRF tokens.
 * Uses Jakarta EE's CSRF mechanism if available, otherwise falls back to
 * generating
 * a secure random token.
 */
public final class CsrfToken {
    private static final Logger logger = Logger.getLogger(CsrfToken.class.getName());
    private static final String CSRF_COOKIE_NAME = "_csrf";

    private CsrfToken() {
    }

    /**
     * Generates a CSRF token using CryptographyUtils. The token is sent to the
     * client
     * using the configured {@link CsrfHandler} from {@link SecurityConfiguration}.
     * <p>
     * The current {@link JoltContext} is retrieved using
     * {@link JoltDispatcherServlet#getCurrentContext()}.
     * 
     * @return The CSRF token, or null if generation fails or no context is
     *         available
     */
    public static String generate() {
        JoltContext context = JoltDispatcherServlet.getCurrentContext();
        if (context == null) {
            logger.warning("No current JoltContext available to generate CSRF token");
            return null;
        }
        CsrfConfiguration config = JoltContainer.getInstance()
                .getBean(SecurityConfiguration.class)
                .getCsrfConfig();
        CsrfHandler handler = config.getHandler();

        String token = CryptographyUtils.randomUrlSafeBase64(32);
        handler.sendToken(context, token, config);
        logger.info(() -> "CSRF token generated and sent via handler: " + token);
        return token;
    }

    /**
     * Generates a new CSRF token after successful validation (token rotation).
     * 
     * @return The new CSRF token
     */
    public static String rotate() {
        JoltContext context = JoltDispatcherServlet.getCurrentContext();
        if (context == null) {
            logger.warning("No current JoltContext available to rotate CSRF token");
            return null;
        }

        CsrfConfiguration config = JoltContainer.getInstance()
                .getBean(SecurityConfiguration.class)
                .getCsrfConfig();
        CsrfHandler handler = config.getHandler();

        String newToken = CryptographyUtils.randomUrlSafeBase64(32);
        handler.sendToken(context, newToken, config);
        logger.fine(() -> "CSRF token rotated: " + newToken);
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

        logger.fine(() -> "CSRF token stored in secure cookie: " + CSRF_COOKIE_NAME + " with value: " + token);
    }

    /**
     * Retrieves the CSRF token from the cookie.
     * 
     * @param context The Jolt context
     * @return The CSRF token, or null if not found
     */
    public static String getFromCookie(JoltContext context) {
        String token = context.getCookieValue(CSRF_COOKIE_NAME).orElse(null);
        logger.fine(() -> "Retrieved CSRF token from cookie: " + (token != null ? token : "none"));
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
            return 1800; // Default 30 minutes if no session
        } catch (Exception e) {
            logger.warning("Failed to retrieve session lifetime, using default: " + e.getMessage());
            return 1800; // Fallback to 30 minutes
        }
    }
}
