package io.github.t1willi.security.cryptography;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import io.github.t1willi.exceptions.JoltSecurityException;

/**
 * Utility class for deriving encryption keys from passwords.
 * This class provides functionality to create AES-256 encryption keys
 * from passwords with optional salt for improved security.
 */
class KeyDerivation {

    /**
     * The algorithm used for key derivation.
     */
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * The number of iterations to use in the key derivation function.
     * Higher values increase security but also computational cost.
     */
    private static final int ITERATION_COUNT = 100000;

    /**
     * The length of the derived key in bits (256 for AES-256).
     */
    private static final int KEY_LENGTH = 256;

    /**
     * Derives an AES-256 encryption key from a password without salt.
     * This method uses an empty string as the salt.
     * Note: For better security, use the overloaded method with a salt parameter.
     * 
     * @param password The password to derive the key from.
     * @return A Base64-encoded string representing the derived AES-256 key.
     * @throws JoltSecurityException If the key derivation process fails.
     */
    public static String deriveKey(String password) {
        return deriveKey(password, "");
    }

    /**
     * Derives an AES-256 encryption key from a password and salt.
     * This method uses PBKDF2 with HMAC-SHA256 to derive a cryptographically
     * strong key from the provided password and salt.
     * 
     * @param password The password to derive the key from.
     * @param salt     The salt to use in the key derivation process. Using a unique
     *                 salt
     *                 for each password greatly enhances security against rainbow
     *                 table attacks.
     * @return A Base64-encoded string representing the derived AES-256 key.
     * @throws JoltSecurityException If the key derivation process fails due to
     *                               algorithm unavailability or invalid key
     *                               specification.
     */
    public static String deriveKey(String password, String salt) {
        try {
            byte[] saltBytes = salt.getBytes();

            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    saltBytes,
                    ITERATION_COUNT,
                    KEY_LENGTH);

            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();

            // Clear sensitive data from memory
            spec.clearPassword();

            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new JoltSecurityException("Error deriving key from password", e);
        }
    }
}