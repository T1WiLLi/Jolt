package io.github.t1willi.security.cryptography;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import io.github.t1willi.exceptions.JoltSecurityException;
import io.github.t1willi.server.config.ConfigurationManager;
import io.github.t1willi.utils.Constant;

/**
 * Utility class for text hashing, verification, encryption, and decryption.
 * It also includes methods for generating and validating strong passwords.
 * This class provides a comprehensive set of cryptographic operations for
 * secure
 * password handling, data encryption, and key derivation.
 * Provides overloads that let users select algorithms and parameters
 * directly:
 * <ul>
 * <li>{@link #hash(String)}</li>
 * <li>{@link #hash(String, String)}</li>
 * <li>{@link #encrypt(String)}</li>
 * <li>{@link #encrypt(String, String, String)}</li>
 * <li>{@link #decrypt(String)}</li>
 * <li>{@link #decrypt(String, String, String)}</li>
 * </ul>
 */
public final class Cryptography {

    // ------------------- Hashing Constants -------------------
    /**
     * PBKDF2 with HMAC-SHA512 hashing algorithm constant (default).
     */
    private static final String PBKDF2_SHA512 = Constant.Security.PBKDF2_SHA512;
    /**
     * Number of iterations for PBKDF2 hashing (high for security).
     */
    private static final int HASH_ITERATIONS = 210000;
    /**
     * Length of the derived hash key in bits (512 bits = 64 bytes).
     */
    private static final int HASH_KEY_LENGTH = 512;
    /**
     * Salt length in bytes for hashing randomization.
     */
    private static final int SALT_LENGTH = 16;

    // ---------------- Encryption Constants -------------------
    /**
     * AES CBC mode with PKCS#5 padding.
     */
    public static final String AES_CBC_PKCS5 = Constant.Security.AES_CBC_PKCS5;
    /**
     * AES CBC mode with no padding (caller must handle block alignment).
     */
    public static final String AES_CBC_NOPADDING = Constant.Security.AES_CBC_NOPADDING;
    /**
     * AES CTR mode with no padding for streaming encryption.
     */
    public static final String AES_CTR_NOPADDING = Constant.Security.AES_CTR_NOPADDING;
    /**
     * AES ECB mode with PKCS#5 padding (not recommended for data patterns).
     */
    public static final String AES_ECB_PKCS5 = Constant.Security.AES_ECB_PKCS5;
    /**
     * AES GCM mode with NoPadding for AEAD (default).
     */
    public static final String AES_GCM_NOPADDING = Constant.Security.AES_GCM_NOPADDING;
    /**
     * Default encryption algorithm if none specified.
     */
    private static final String DEFAULT_ENCRYPTION_ALGORITHM = AES_GCM_NOPADDING;
    /**
     * Authentication tag length in bits for GCM mode (128 bits = 16 bytes).
     */
    private static final int GCM_TAG_LENGTH = 128;
    /**
     * Initialization Vector (IV) length in bytes for GCM mode (12 bytes).
     */
    private static final int GCM_IV_LENGTH = 12;

    // ------------- Project Key & Pepper ------------------
    /**
     * Global project-wide encryption key (Base64). Loaded from
     * configuration or generated once if absent. Must be securely stored.
     */
    private static final String PROJECT_SECRET_KEY = ConfigurationManager.getInstance()
            .getProperty("server.security.secret_key", CryptographyUtils.randomBase64(32));
    /**
     * Global pepper appended to all passwords before hashing to
     * mitigate precomputed attacks. Loaded or generated if absent.
     */
    private static final String PEPPER = ConfigurationManager.getInstance()
            .getProperty("server.security.pepper", CryptographyUtils.randomBase64(32));

    /**
     * Hashes a text using PBKDF2 with a salt and a pepper.
     * The method generates a random salt, adds the pepper to the text,
     * and then creates a hash using the specified algorithm.
     *
     * @param text the text to hash
     * @return a Base64-encoded string containing the salt and the hashed text
     * @throws JoltSecurityException if hashing fails
     */
    public static String hash(String text) {
        try {
            byte[] salt = SecureRandomGenerator.generateRandomBytes(SALT_LENGTH);

            String pepperedPassword = text + PEPPER;

            PBEKeySpec spec = new PBEKeySpec(
                    pepperedPassword.toCharArray(),
                    salt,
                    HASH_ITERATIONS,
                    HASH_KEY_LENGTH);

            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_SHA512);
            byte[] hash = skf.generateSecret(spec).getEncoded();

            spec.clearPassword();
            ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + salt.length + hash.length);

            buffer.putInt(HASH_ITERATIONS);
            buffer.putInt(salt.length);
            buffer.put(salt);
            buffer.put(hash);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new JoltSecurityException("Error hashing password", e);
        }
    }

    /**
     * Verifies if the provided text matches the stored hash.
     * This method extracts the salt and hash from the stored hash value,
     * adds the pepper to the provided password, and compares the generated
     * hash with the stored hash using a constant-time comparison.
     *
     * @param storedHash the Base64-encoded hash to verify against
     * @param text       the text to verify
     * @return true if the password matches the stored hash, false otherwise
     * @throws JoltSecurityException if verification fails
     */
    public static boolean verify(String storedHash, String text) {
        try {
            byte[] decodedHash = Base64.getDecoder().decode(storedHash);
            ByteBuffer buffer = ByteBuffer.wrap(decodedHash);

            int iterations = buffer.getInt();
            int saltLength = buffer.getInt();

            byte[] salt = new byte[saltLength];
            buffer.get(salt);

            byte[] originalHash = new byte[buffer.remaining()];
            buffer.get(originalHash);

            String pepperedPassword = text + PEPPER;

            PBEKeySpec spec = new PBEKeySpec(
                    pepperedPassword.toCharArray(),
                    salt,
                    iterations,
                    originalHash.length * 8);

            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_SHA512);
            byte[] hash = skf.generateSecret(spec).getEncoded();

            spec.clearPassword();

            return constantTimeEquals(originalHash, hash);
        } catch (Exception e) {
            throw new JoltSecurityException("Error verifying password", e);
        }
    }

    /**
     * Encrypt plaintext using default algorithm (AES/GCM/NoPadding)
     * and global project key. Produces authenticated ciphertext.
     *
     * @param plaintext UTF-8 text to encrypt
     * @return Base64-encoded [IV|ciphertext|tag]
     */
    public static String encrypt(String plaintext) {
        return encrypt(plaintext, DEFAULT_ENCRYPTION_ALGORITHM, PROJECT_SECRET_KEY);
    }

    /**
     * Encrypt plaintext using a specific AES mode with global key.
     * Supports AES_CBC_PKCS5, AES_CTR_NOPADDING, etc.
     *
     * @param plaintext text to encrypt
     * @param algorithm AES mode constant
     * @return Base64-encoded [IV|ciphertext] or [IV|ciphertext|tag]
     */
    public static String encrypt(String plaintext, String algorithm) {
        return encrypt(plaintext, algorithm, PROJECT_SECRET_KEY);
    }

    /**
     * Encrypt plaintext using specified algorithm and Base64 key.
     * Automatically handles IV size and AEAD if GCM is selected.
     *
     * @param plaintext text to encrypt
     * @param algorithm full cipher transformation string
     * @param base64Key AES key in Base64 format
     * @return Base64-encoded payload [IV|ciphertext|tag]
     */
    public static String encrypt(String plaintext, String algorithm, String base64Key) {
        try {
            int ivLen = algorithm.contains("GCM") ? GCM_IV_LENGTH : 16;
            byte[] iv = SecureRandomGenerator.generateRandomBytes(ivLen);
            SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES");
            Cipher cipher = Cipher.getInstance(algorithm);
            if (algorithm.contains("GCM")) {
                cipher.init(Cipher.ENCRYPT_MODE, key,
                        new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            } else {
                SecureRandom secureRandom = new SecureRandom();
                byte[] dynamicIv = new byte[16];
                secureRandom.nextBytes(iv);
                cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(dynamicIv));
            }
            byte[] ct = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length);
            buf.put(iv).put(ct);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new JoltSecurityException("Error encrypting data", e);
        }
    }

    /**
     * Decrypt ciphertext using default algorithm and global key.
     *
     * @param base64Data Base64-encoded [IV|ciphertext|tag]
     * @return decrypted UTF-8 plaintext
     */
    public static String decrypt(String base64Data) {
        return decrypt(base64Data, DEFAULT_ENCRYPTION_ALGORITHM, PROJECT_SECRET_KEY);
    }

    /**
     * Decrypt using specified algorithm and global key.
     *
     * @param base64Data encoded payload
     * @param algorithm  AES mode to use
     * @return plaintext
     */
    public static String decrypt(String base64Data, String algorithm) {
        return decrypt(base64Data, algorithm, PROJECT_SECRET_KEY);
    }

    /**
     * Decrypt using specified algorithm and Base64 key.
     * Handles IV extraction and AEAD if GCM.
     *
     * @param base64Data encoded [IV|ciphertext|tag]
     * @param algorithm  transformation string
     * @param base64Key  AES key
     * @return decrypted text
     */
    public static String decrypt(String base64Data, String algorithm, String base64Key) {
        try {
            byte[] blob = Base64.getDecoder().decode(base64Data);
            ByteBuffer buf = ByteBuffer.wrap(blob);
            int ivLen = algorithm.contains("GCM") ? GCM_IV_LENGTH : 16;
            byte[] iv = new byte[ivLen];
            buf.get(iv);
            byte[] ct = new byte[buf.remaining()];
            buf.get(ct);
            SecretKey key = new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES");
            Cipher cipher = Cipher.getInstance(algorithm);
            if (algorithm.contains("GCM")) {
                cipher.init(Cipher.DECRYPT_MODE, key,
                        new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            } else {
                cipher.init(Cipher.DECRYPT_MODE, key,
                        new IvParameterSpec(iv));
            }
            byte[] pt = cipher.doFinal(ct);
            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new JoltSecurityException("Error decrypting data", e);
        }
    }

    /**
     * Derives an AES-256 encryption key from a password and salt.
     * This method delegates to {@link KeyDerivation#deriveKey(String, String)}
     * which uses
     * PBKDF2 with HMAC-SHA256 to derive a cryptographically strong key
     * suitable for encryption operations.
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
        return KeyDerivation.deriveKey(password, salt);
    }

    /**
     * Derives an AES-256 encryption key from a password without salt.
     * This method delegates to {@link KeyDerivation#deriveKey(String)} which uses
     * an empty string as the salt.
     * 
     * Note: For better security, use the overloaded method with a salt parameter.
     * 
     * @param password The password to derive the key from.
     * @return A Base64-encoded string representing the derived AES-256 key.
     * @throws JoltSecurityException If the key derivation process fails.
     * @see #deriveKey(String, String)
     */
    public static String deriveKey(String password) {
        return KeyDerivation.deriveKey(password);
    }

    /**
     * Generates a cryptographically secure random password of the specified length.
     * The generated password will contain at least one uppercase letter, one
     * lowercase letter,
     * one digit, and one special character. The remaining characters are randomly
     * selected
     * from all character sets. The characters are then shuffled to ensure
     * randomness.
     *
     * @param length the length of the password to generate (must be at least 4)
     * @return a random password meeting the complexity requirements
     * @throws IllegalArgumentException if length is less than 4
     */
    public static String generateRandomPassword(int length) {
        String uppercase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowercase = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*()-_=+[]{}|;:,.<>?";

        String allChars = uppercase + lowercase + digits + special;
        StringBuilder password = new StringBuilder();

        password.append(CryptographyUtils.randomString(1, uppercase));
        password.append(CryptographyUtils.randomString(1, lowercase));
        password.append(CryptographyUtils.randomString(1, digits));
        password.append(CryptographyUtils.randomString(1, special));
        password.append(CryptographyUtils.randomString(length - 4, allChars));

        char[] passwordChars = password.toString().toCharArray();
        for (int i = passwordChars.length - 1; i > 0; i--) {
            int index = SecureRandomGenerator.generateRandomInt(0, i + 1);
            char temp = passwordChars[index];
            passwordChars[index] = passwordChars[i];
            passwordChars[i] = temp;
        }
        return new String(passwordChars);
    }

    /**
     * Validates if a password meets the strong password criteria.
     * A strong password must:
     * - Be at least 8 characters long
     * - Contain at least one uppercase letter
     * - Contain at least one lowercase letter
     * - Contain at least one digit
     * - Contain at least one special character (non-alphanumeric)
     *
     * @param password the password to validate
     * @return true if the password meets all criteria, false otherwise
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowercase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else {
                hasSpecial = true;
            }
        }

        return hasUppercase && hasLowercase && hasDigit && hasSpecial;
    }

    /**
     * Compares two byte arrays in constant time to prevent timing attacks.
     * This method performs a bitwise XOR comparison of all bytes in both arrays
     * and returns true only if all bytes are equal. The comparison time is constant
     * regardless of where the first difference occurs, making it resistant to
     * timing attacks that could reveal information about the contents.
     *
     * @param a the first byte array
     * @param b the second byte array
     * @return true if the arrays have the same length and content, false otherwise
     */
    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}