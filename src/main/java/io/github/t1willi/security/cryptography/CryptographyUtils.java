package io.github.t1willi.security.cryptography;

import java.util.Base64;

/**
 * Utility class for cryptographic operations, primarily focused on generating
 * secure random values in various formats.
 */
public final class CryptographyUtils {

    private static final String HEX_CHARS = "0123456789ABCDEF";
    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private CryptographyUtils() {
    }

    /**
     * Generates a random hexadecimal string of the specified length.
     *
     * @param length The length of the hexadecimal string to generate
     * @return A random hexadecimal string
     * @throws IllegalArgumentException if length is less than or equal to zero
     */
    public static String randomHex(int length) {
        return randomString(length, HEX_CHARS);
    }

    /**
     * Generates a random string of the specified length using the given character
     * set.
     *
     * @param length The length of the string to generate
     * @param chars  The character set to use for generating the random string
     * @return A random string composed of characters from the provided character
     *         set
     * @throws IllegalArgumentException if length is less than or equal to zero or
     *                                  if chars is null or empty
     */
    public static String randomString(int length, String chars) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than zero");
        }
        if (chars == null || chars.isEmpty()) {
            throw new IllegalArgumentException("Characters must not be null or empty");
        }

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomIndex = SecureRandomGenerator.generateRandomInt(0, chars.length());
            sb.append(chars.charAt(randomIndex));
        }
        return sb.toString();
    }

    /**
     * Generates a random alphanumeric string of the specified length.
     * The string will contain characters from the set [0-9A-Za-z].
     *
     * @param length The length of the alphanumeric string to generate
     * @return A random alphanumeric string
     * @throws IllegalArgumentException if length is less than or equal to zero
     */
    public static String randomAlphanumeric(int length) {
        return randomString(length, ALPHANUMERIC);
    }

    /**
     * Generates an array of random bytes of the specified length.
     *
     * @param length The number of random bytes to generate
     * @return An array containing the random bytes
     */
    public static byte[] randomBytes(int length) {
        return SecureRandomGenerator.generateRandomBytes(length);
    }

    /**
     * Generates a Base64-encoded string from random bytes.
     *
     * @param byteLength The number of random bytes to generate before encoding
     * @return A Base64-encoded string of the random bytes
     */
    public static String randomBase64(int byteLength) {
        byte[] bytes = randomBytes(byteLength);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Generates a URL-safe Base64-encoded string from random bytes, without
     * padding.
     * This format is suitable for use in URLs and filenames.
     *
     * @param byteLength The number of random bytes to generate before encoding
     * @return A URL-safe Base64-encoded string of the random bytes, without padding
     */
    public static String randomUrlSafeBase64(int byteLength) {
        byte[] bytes = randomBytes(byteLength);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}