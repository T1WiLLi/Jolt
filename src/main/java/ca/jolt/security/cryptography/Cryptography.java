package ca.jolt.security.cryptography;

import java.util.Base64;

public final class Cryptography {

    private static final String HEX_CHARS = "0123456789ABCDEF";
    private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    public static String randomHex(int length) {
        return randomString(length, HEX_CHARS);
    }

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

    public static String randomAlphanumeric(int length) {
        return randomString(length, ALPHANUMERIC);
    }

    public static byte[] randomBytes(int length) {
        return SecureRandomGenerator.generateRandomBytes(length);
    }

    public static String randomBase64(int byteLength) {
        byte[] bytes = randomBytes(byteLength);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String randomUrlSafeBase64(int byteLength) {
        byte[] bytes = randomBytes(byteLength);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
