package io.github.t1willi.security.cryptography;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import io.github.t1willi.exceptions.JoltSecurityException;
import io.github.t1willi.server.config.ConfigurationManager;
import io.github.t1willi.utils.Constant;

/**
 * <p>
 * Provides cryptographic utilities for secure password hashing, message
 * authentication,
 * and data confidentiality. This class supports:
 * </p>
 * <ul>
 * <li>PBKDF2-based hashing with random or user-supplied salts</li>
 * <li>HMAC-SHA256 generation for message authentication codes</li>
 * <li>AES/GCM encryption and decryption for confidentiality and integrity</li>
 * <li>Deriving keys via PBKDF2-HMAC-SHA256 for external use</li>
 * <li>Random password generation and strength validation</li>
 * <li>Unified verification API to detect credential format and perform correct
 * checks</li>
 * </ul>
 * <p>
 * All methods throw {@link JoltSecurityException} on internal failures,
 * and {@link IllegalArgumentException} for invalid arguments.
 * </p>
 *
 * @since 1.0
 */
public final class Cryptography {

    private Cryptography() {
        // Utility class; prevent instantiation
    }

    // ----- Configuration parameters -----
    private static final String HASH_ALGORITHM = Constant.Security.PBKDF2_SHA512;
    private static final int HASH_ITERATIONS = 210_000;
    private static final int HASH_KEY_LENGTH = 512;
    private static final int SALT_LENGTH = 16;

    private static final String ENCRYPTION_ALGORITHM = Constant.Security.AES_GCM_NOPADDING;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private static final String PROJECT_SECRET_KEY;
    private static final String PEPPER;

    static {
        ConfigurationManager cfg = ConfigurationManager.getInstance();
        PROJECT_SECRET_KEY = cfg.getProperty(
                "server.security.secret_key",
                CryptographyUtils.randomBase64(32));
        PEPPER = cfg.getProperty(
                "server.security.pepper",
                CryptographyUtils.randomBase64(32));
        validateKeyLength(PROJECT_SECRET_KEY);
    }

    /**
     * Generates a PBKDF2 hash of the given text using a random salt and configured
     * pepper.
     * 
     * @param text plaintext to hash (not null)
     * @return "PBKDF2$<base64>" blob containing iteration, salt, and hash
     */
    public static String hash(String text) {
        if (text == null)
            throw new IllegalArgumentException("Text to hash cannot be null");
        byte[] salt = SecureRandomGenerator.generateRandomBytes(SALT_LENGTH);
        return hash(text, salt);
    }

    /**
     * Generates a PBKDF2 hash of the given text using a salt string (UTF-8) and
     * configured pepper.
     * 
     * @param text       plaintext to hash (not null)
     * @param saltString salt as UTF-8 string (not null/empty)
     * @return "PBKDF2$<base64>" blob
     */
    public static String hash(String text, String saltString) {
        if (text == null)
            throw new IllegalArgumentException("Text to hash cannot be null");
        if (saltString == null || saltString.isEmpty())
            throw new IllegalArgumentException("Salt string cannot be null or empty");
        return hash(text, saltString.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a PBKDF2 hash using supplied salt bytes and configured pepper.
     * 
     * @param text plaintext to hash (not null)
     * @param salt salt bytes (not null/empty)
     * @return "PBKDF2$<base64>" blob
     */
    public static String hash(String text, byte[] salt) {
        if (text == null)
            throw new IllegalArgumentException("Text to hash cannot be null");
        if (salt == null || salt.length == 0)
            throw new IllegalArgumentException("Salt cannot be null or empty");
        try {
            String peppered = text + PEPPER;
            PBEKeySpec spec = new PBEKeySpec(
                    peppered.toCharArray(), salt, HASH_ITERATIONS, HASH_KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(HASH_ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            spec.clearPassword();

            ByteBuffer buf = ByteBuffer.allocate(4 + 4 + salt.length + hash.length);
            buf.putInt(HASH_ITERATIONS).putInt(salt.length).put(salt).put(hash);
            return "PBKDF2$" + encodeBase64(buf.array());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new JoltSecurityException("Error computing PBKDF2 hash", e);
        }
    }

    /**
     * Computes an HMAC-SHA256 digest of the given text.
     * 
     * @param text message to authenticate (not null)
     * @return "HMAC$<base64>" of 32-byte MAC
     */
    public static String hmac(String text) {
        if (text == null)
            throw new IllegalArgumentException("Text for HMAC cannot be null");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    PEPPER.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(text.getBytes(StandardCharsets.UTF_8));
            return "HMAC$" + encodeBase64(digest);
        } catch (Exception e) {
            throw new JoltSecurityException("Error computing HMAC-SHA256", e);
        }
    }

    /**
     * Verifies a stored PBKDF2 or HMAC credential against plaintext.
     * 
     * @param stored "PBKDF2$..." or "HMAC$..."
     * @param text   plaintext to verify (not null)
     * @return true if matches
     */
    public static boolean verify(String stored, String text) {
        if (stored == null)
            throw new IllegalArgumentException("Stored credential cannot be null");
        if (text == null)
            throw new IllegalArgumentException("Text to verify cannot be null");
        if (stored.startsWith("PBKDF2$"))
            return verifyPbkdf2(stored.substring(7), text);
        if (stored.startsWith("HMAC$"))
            return verifyHmac(stored.substring(5), text);
        throw new JoltSecurityException("Unrecognized credential format: " + stored);
    }

    /**
     * Derives a key via PBKDF2-HMAC-SHA256 by delegating to {@link KeyDerivation}.
     * 
     * @param password input password (not null)
     * @param salt     salt text (not null)
     * @return Base64-encoded AES-256 key
     */
    public static String deriveKey(String password, String salt) {
        return KeyDerivation.deriveKey(password, salt);
    }

    /**
     * Derives a key via PBKDF2-HMAC-SHA256 with full parameter control.
     * 
     * @param password   input password
     * @param salt       salt string
     * @param algorithm  PBKDF2 algorithm constant
     * @param iterations number of rounds
     * @param keyLength  desired key length in bits
     * @return Base64-encoded key bytes
     */
    public static String deriveKey(
            String password, String salt,
            String algorithm, int iterations, int keyLength) {
        return KeyDerivation.deriveKey(password, salt, algorithm, iterations, keyLength);
    }

    /**
     * Generates a cryptographically secure random password of given length.
     * 
     * @param length total length >=4
     * @return random password meeting complexity: upper, lower, digit, special
     */
    public static String generateRandomPassword(int length) {
        if (length < 4)
            throw new IllegalArgumentException("Password length must be at least 4");
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*()-_=+[]{}|;:,.<>?";
        String all = upper + lower + digits + special;
        StringBuilder sb = new StringBuilder();
        sb.append(CryptographyUtils.randomString(1, upper));
        sb.append(CryptographyUtils.randomString(1, lower));
        sb.append(CryptographyUtils.randomString(1, digits));
        sb.append(CryptographyUtils.randomString(1, special));
        sb.append(CryptographyUtils.randomString(length - 4, all));
        char[] arr = sb.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = SecureRandomGenerator.generateRandomInt(0, i + 1);
            char tmp = arr[j];
            arr[j] = arr[i];
            arr[i] = tmp;
        }
        return new String(arr);
    }

    /**
     * Validates password strength: min 8, has upper, lower, digit, special.
     * 
     * @param password to test
     * @return true if strong
     */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8)
            return false;
        boolean u = false, l = false, d = false, s = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c))
                u = true;
            else if (Character.isLowerCase(c))
                l = true;
            else if (Character.isDigit(c))
                d = true;
            else
                s = true;
        }
        return u && l && d && s;
    }

    /**
     * Encrypts plaintext using AES/GCM with project key.
     */
    public static String encrypt(String plain) {
        return encrypt(plain, PROJECT_SECRET_KEY);
    }

    /**
     * Encrypts with provided Base64 key
     */
    public static String encrypt(String plain, String base64Key) {
        if (plain == null || base64Key == null)
            throw new IllegalArgumentException("Invalid args");
        try {
            byte[] iv = SecureRandomGenerator.generateRandomBytes(GCM_IV_LENGTH);
            byte[] key = decodeBase64(base64Key);
            Cipher c = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return encodeBase64(ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array());
        } catch (Exception e) {
            throw new JoltSecurityException("Error encrypting", e);
        }
    }

    /**
     * Decrypts using project key
     */
    public static String decrypt(String encrypted) {
        return decrypt(encrypted, PROJECT_SECRET_KEY);
    }

    /**
     * Decrypts with provided Base64 key
     */
    public static String decrypt(String encrypted, String base64Key) {
        if (encrypted == null || base64Key == null)
            throw new IllegalArgumentException("Invalid args");
        try {
            ByteBuffer buf = ByteBuffer.wrap(decodeBase64(encrypted));
            byte[] iv = new byte[GCM_IV_LENGTH];
            buf.get(iv);
            byte[] ct = new byte[buf.remaining()];
            buf.get(ct);
            Cipher c = Cipher.getInstance(ENCRYPTION_ALGORITHM);
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(decodeBase64(base64Key), "AES"),
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(c.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new JoltSecurityException("Error decrypting", e);
        }
    }

    // ----- Private helpers -----

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length)
            return false;
        int res = 0;
        for (int i = 0; i < a.length; i++)
            res |= a[i] ^ b[i];
        return res == 0;
    }

    private static boolean verifyPbkdf2(String blob, String text) {
        try {
            ByteBuffer buf = ByteBuffer.wrap(decodeBase64(blob));
            int iter = buf.getInt(), sl = buf.getInt();
            byte[] salt = new byte[sl];
            buf.get(salt);
            byte[] orig = new byte[buf.remaining()];
            buf.get(orig);
            String p = text + PEPPER;
            PBEKeySpec s = new PBEKeySpec(p.toCharArray(), salt, iter, orig.length * 8);
            byte[] c = SecretKeyFactory.getInstance(HASH_ALGORITHM).generateSecret(s).getEncoded();
            s.clearPassword();
            return constantTimeEquals(orig, c);
        } catch (Exception e) {
            throw new JoltSecurityException("Error verifying PBKDF2 blob", e);
        }
    }

    private static boolean verifyHmac(String macBlob, String text) {
        try {
            byte[] a = decodeBase64(macBlob);
            byte[] e = decodeBase64(hmac(text).substring(5));
            return constantTimeEquals(a, e);
        } catch (Exception e) {
            throw new JoltSecurityException("Error verifying HMAC blob", e);
        }
    }

    private static byte[] decodeBase64(String d) {
        if (d == null || d.isEmpty())
            throw new IllegalArgumentException("Invalid Base64");
        return Base64.getDecoder().decode(d);
    }

    private static String encodeBase64(byte[] d) {
        return Base64.getEncoder().encodeToString(d);
    }

    private static void validateKeyLength(String k) {
        byte[] b = decodeBase64(k);
        int l = b.length;
        if (!(l == 16 || l == 24 || l == 32))
            throw new JoltSecurityException("Invalid AES key length: " + l);
    }
}
