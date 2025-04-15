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
 */
public final class Cryptography {

    private static final String HASH_ALGORITHM = "PBKDF2WithHmacSHA512";
    private static final int HASH_ITERATIONS = 210000;
    private static final int HASH_KEY_LENGTH = 512;
    private static final int SALT_LENGTH = 16;

    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // 16 bytes
    private static final int GCM_IV_LENGTH = 12;

    private static final String PROJECT_SECRET_KEY = ConfigurationManager.getInstance().getProperty(
            "server.security.secret_key",
            CryptographyUtils.randomBase64(32));

    private static final String PEPPER = ConfigurationManager.getInstance().getProperty(
            "server.security.pepper",
            CryptographyUtils.randomBase64(32));

    /**
     * Hashes a password using PBKDF2 with a salt and a pepper.
     *
     * @param text the text to hash
     * @return a Base64-encoded string containing the salt and the hashed password
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
     * Verifies if the provided password matches the stored hash.
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

    public static String encrypt(String text) {
        return encrypt(text, PROJECT_SECRET_KEY);
    }

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

    public static String decrypt(String encryptedText) {
        return decrypt(encryptedText, PROJECT_SECRET_KEY);
    }

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
