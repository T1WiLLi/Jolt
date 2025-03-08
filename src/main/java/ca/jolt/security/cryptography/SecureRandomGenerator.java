package ca.jolt.security.cryptography;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import ca.jolt.exceptions.JoltSecurityException;

/**
 * Internal utility class for generating cryptographically secure random values.
 * This class provides access to a properly initialized SecureRandom instance
 * and convenience methods for common random generation operations.
 */
final class SecureRandomGenerator {

    /**
     * The shared SecureRandom instance that uses the strongest algorithm available.
     * Initialized statically to avoid repeated expensive initialization.
     */
    private static final SecureRandom SECURE_RANDOM;

    private SecureRandomGenerator() {
    }

    static {
        try {
            SECURE_RANDOM = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new JoltSecurityException("Failed to initialize secure random generator", e);
        }
    }

    /**
     * Generates an array of random bytes of the specified length.
     *
     * @param length The number of random bytes to generate
     * @return A byte array filled with cryptographically secure random bytes
     */
    public static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    /**
     * Generates a random integer within the specified range [min, max).
     * The range is inclusive of min and exclusive of max.
     *
     * @param min The minimum value (inclusive)
     * @param max The maximum value (exclusive)
     * @return A random integer between min (inclusive) and max (exclusive)
     * @throws IllegalArgumentException if min is greater than or equal to max
     */
    public static int generateRandomInt(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("min must be less than max");
        }
        return SECURE_RANDOM.nextInt(max - min) + min;
    }

    /**
     * Provides access to the underlying SecureRandom instance.
     *
     * @return The shared SecureRandom instance
     */
    public static SecureRandom getSecureRandom() {
        return SECURE_RANDOM;
    }
}