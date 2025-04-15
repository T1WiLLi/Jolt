package io.github.t1willi.security.cryptography;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import io.github.t1willi.exceptions.JoltSecurityException;
import io.github.t1willi.server.config.ConfigurationManager;

/**
 * Utility class for text hashing, verification, encryption, and decryption.
 * It also includes methods for generating and validating strong passwords.
 * This class provides a comprehensive set of cryptographic operations for
 * secure
 * password handling, data encryption, and key derivation.
 */
public final class Cryptography {

    /**
     * The algorithm used for password hashing.
     */
    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA512";

    /**
     * The number of iterations used in the password hashing algorithm.
     * Higher values increase security but also computational cost.
     */
    private static final int HASH_ITERATIONS = 210000;

    /**
     * The length of the hash key in bits.
     */
    private static final int HASH_KEY_LENGTH = 512;

    /**
     * The length of the salt in bytes used for password hashing.
     */
    private static final int SALT_LENGTH = 16;

    /**
     * The algorithm used for data encryption/decryption.
     * AES/GCM/NoPadding provides authenticated encryption with associated data
     * (AEAD).
     */
    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";

    /**
     * The authentication tag length in bits for GCM mode.
     */
    private static final int GCM_TAG_LENGTH = 128; // 16 bytes

    /**
     * The initialization vector length in bytes for GCM mode.
     */
    private static final int GCM_IV_LENGTH = 12;

    /**
     * The project-wide secret key used for encryption/decryption operations.
     * This key is loaded from configuration or generated randomly if not found.
     */
    private static final String PROJECT_SECRET_KEY = ConfigurationManager.getInstance().getProperty(
            "server.security.secret_key",
            CryptographyUtils.randomBase64(32));

    /**
     * A secret value added to passwords before hashing for additional security.
     * This pepper is loaded from configuration or generated randomly if not found.
     */
    private static final String PEPPER = ConfigurationManager.getInstance().getProperty(
            "server.security.pepper",
            CryptographyUtils.randomBase64(32));

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

            SecretKeyFactory skf = SecretKeyFactory.getInstance(HASH_ALGORITHM);
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

            SecretKeyFactory skf = SecretKeyFactory.getInstance(HASH_ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();

            spec.clearPassword();

            return constantTimeEquals(originalHash, hash);
        } catch (Exception e) {
            throw new JoltSecurityException("Error verifying password", e);
        }
    }

    /**
     * Encrypts the provided text using AES-GCM with the specified key.
     * This method generates a random initialization vector (IV) and
     * uses the GCM mode for authenticated encryption.
     *
     * @param text the text to encrypt
     * @param key  the Base64-encoded encryption key
     * @return a Base64-encoded string containing the IV and encrypted data
     * @throws JoltSecurityException if encryption fails
     */
    public static String encrypt(String text, String key) {
        try {
            byte[] iv = SecureRandomGenerator.generateRandomBytes(GCM_IV_LENGTH);

            SecretKey secretKey = new SecretKeySpec(
                    Base64.getDecoder().decode(key),
                    "AES");

            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] passwordBytes = text.getBytes(StandardCharsets.UTF_8);
            byte[] encryptedBytes = cipher.doFinal(passwordBytes);

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedBytes);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new JoltSecurityException("Error encrypting password", e);
        }
    }

    /**
     * Encrypts the provided text using AES-GCM with the project's secret key.
     * This is a convenience method that uses the project-wide secret key for
     * encryption.
     *
     * @param text the text to encrypt
     * @return a Base64-encoded string containing the IV and encrypted data
     * @throws JoltSecurityException if encryption fails
     * @see #encrypt(String, String)
     */
    public static String encrypt(String text) {
        return encrypt(text, PROJECT_SECRET_KEY);
    }

    /**
     * Decrypts the provided encrypted text using AES-GCM with the specified key.
     * This method extracts the IV from the encrypted data and uses it along with
     * the key to decrypt the data.
     *
     * @param encryptedText the Base64-encoded encrypted text
     * @param key           the Base64-encoded decryption key
     * @return the decrypted text as a string
     * @throws JoltSecurityException if decryption fails
     */
    public static String decrypt(String encryptedText, String key) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);

            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedBytes);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            SecretKey secretKey = new SecretKeySpec(
                    Base64.getDecoder().decode(key),
                    "AES");

            Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedBytes = cipher.doFinal(ciphertext);

            return new String(decryptedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new JoltSecurityException("Error decrypting password", e);
        }
    }

    /**
     * Decrypts the provided encrypted text using AES-GCM with the project's secret
     * key.
     * This is a convenience method that uses the project-wide secret key for
     * decryption.
     *
     * @param encryptedText the Base64-encoded encrypted text
     * @return the decrypted text as a string
     * @throws JoltSecurityException if decryption fails
     * @see #decrypt(String, String)
     */
    public static String decrypt(String encryptedText) {
        return decrypt(encryptedText, PROJECT_SECRET_KEY);
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