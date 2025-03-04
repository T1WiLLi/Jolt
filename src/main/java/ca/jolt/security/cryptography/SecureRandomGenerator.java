package ca.jolt.security.cryptography;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

final class SecureRandomGenerator {

    private static final SecureRandom SECURE_RANDOM;

    static {
        try {
            SECURE_RANDOM = SecureRandom.getInstanceStrong();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to initialize secure random generator", e);
        }
    }

    public static byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    public static int generateRandomInt(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("min must be less than max");
        }
        return SECURE_RANDOM.nextInt(max - min) + min;
    }

    public static SecureRandom getSecureRandom() {
        return SECURE_RANDOM;
    }
}
