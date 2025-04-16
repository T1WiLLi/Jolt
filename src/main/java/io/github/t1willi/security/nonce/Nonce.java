package io.github.t1willi.security.nonce;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Utility class for managing nonces in the Jolt framework.
 * A nonce (number used once) is a random string generated for each request
 * to be used with Content Security Policy (CSP) to secure inline scripts.
 */
public final class Nonce {
    private static final Logger logger = Logger.getLogger(Nonce.class.getName());
    private static final ThreadLocal<String> NONCE_HOLDER = new ThreadLocal<>();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private Nonce() {
        // Prevent instantiation
    }

    /**
     * Generates a new nonce and stores it in the current thread's context.
     * 
     * @return The generated nonce.
     */
    public static String generate() {
        byte[] randomBytes = new byte[16];
        SECURE_RANDOM.nextBytes(randomBytes);
        String nonce = Base64.getEncoder().encodeToString(randomBytes).replace("=", "");
        NONCE_HOLDER.set(nonce);
        logger.info("Generated new nonce for request. New nonce: " + nonce);
        return nonce;
    }

    /**
     * Retrieves the current nonce for the request.
     * 
     * @return The current nonce, or null if none has been generated.
     */
    public static String get() {
        String nonce = NONCE_HOLDER.get();
        if (nonce == null) {
            logger.warning("No nonce available for the current request.");
        }
        return nonce;
    }

    /**
     * Clears the current nonce from the thread's context.
     */
    public static void clear() {
        NONCE_HOLDER.remove();
        logger.fine("Nonce cleared for current request.");
    }
}