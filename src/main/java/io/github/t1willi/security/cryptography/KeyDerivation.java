package io.github.t1willi.security.cryptography;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import io.github.t1willi.exceptions.JoltSecurityException;
import io.github.t1willi.utils.Constant;

/**
 * Utility class for deriving encryption keys from passwords.
 * This class provides functionality to create AES-256 encryption keys
 * from passwords with optional salt for improved security.
 */
class KeyDerivation {
    /**
     * PBKDF2 with HMAC-SHA256 constant - using SHA256 instead of SHA512 for wider
     * compatibility.
     */
    public static final String PBKDF2_SHA256 = Constant.Security.PBKDF2_SHA256;

    /**
     * The number of iterations to use in the key derivation function.
     * Higher values increase security but also computational cost.
     */
    private static final int DEFAULT_ITERATIONS = 100000;

    /**
     * The length of the derived key in bits (256 for AES-256).
     */
    private static final int DEFAULT_KEY_LENGTH = 256;

    /**
     * Derive a 256-bit AES key from password with provided salt.
     * 
     * @param password input password
     * @param salt     salt string for entropy
     * @return Base64-encoded AES key
     */
    public static String deriveKey(String password, String salt) {
        return deriveKey(password, salt, PBKDF2_SHA256,
                DEFAULT_ITERATIONS, DEFAULT_KEY_LENGTH);
    }

    /**
     * Derive key with full parameters control.
     * 
     * @param password   input password
     * @param salt       salt string
     * @param algorithm  PBKDF2 algorithm constant
     * @param iterations number of rounds
     * @param keyLength  output key length in bits
     * @return Base64-encoded key bytes suitable for AES
     */
    public static String deriveKey(
            String password, String salt,
            String algorithm, int iterations, int keyLength) {
        try {
            if (salt.trim().isEmpty() || salt.isBlank() || salt == null) {
                throw new IllegalArgumentException("Salt for key derivation can't be empty !");
            }

            byte[] saltBytes = salt.getBytes(StandardCharsets.UTF_8);

            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(), saltBytes, iterations, keyLength);

            SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
            byte[] keyBytes = skf.generateSecret(spec).getEncoded();
            spec.clearPassword();

            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            return Base64.getEncoder().encodeToString(key.getEncoded());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new JoltSecurityException("Key derivation failed: " + e.getMessage(), e);
        }
    }
}