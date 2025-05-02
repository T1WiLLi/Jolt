package io.github.t1willi.cryptography;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.github.t1willi.exceptions.JoltSecurityException;
import io.github.t1willi.security.cryptography.Cryptography;
import io.github.t1willi.security.cryptography.CryptographyUtils;
import io.github.t1willi.server.config.ConfigurationManager;

/**
 * Comprehensive test suite for the Cryptography utility class.
 * Tests all major functionality including hashing, verification,
 * encryption/decryption with AES/GCM/NoPadding, key derivation,
 * password generation, and validation.
 */
public class CryptographyTests {

        private static final String TEST_PASSWORD = "TestPassword123!";
        private static final String TEST_TEXT = "This is some sensitive data that needs to be encrypted securely!";
        private static final String TEST_SALT = "TestSalt12345";
        private static final String VALID_BASE64_KEY = CryptographyUtils.randomBase64(32);

        @BeforeAll
        public static void setup() {
                // Mock ConfigurationManager to return valid Base64 strings
                try (MockedStatic<ConfigurationManager> mockedConfig = mockStatic(ConfigurationManager.class)) {
                        ConfigurationManager configInstance = mock(ConfigurationManager.class);
                        when(configInstance.getProperty(eq("server.security.secret_key"), anyString()))
                                        .thenReturn(VALID_BASE64_KEY);
                        when(configInstance.getProperty(eq("server.security.pepper"), anyString()))
                                        .thenReturn(VALID_BASE64_KEY);
                        mockedConfig.when(ConfigurationManager::getInstance).thenReturn(configInstance);
                }
        }

        @Test
        @DisplayName("Test password hashing and verification")
        public void testHashAndVerify() {
                String hashedPassword = Cryptography.hash(TEST_PASSWORD);

                assertTrue(Cryptography.verify(hashedPassword, TEST_PASSWORD),
                                "Password verification should succeed with correct password");

                assertFalse(Cryptography.verify(hashedPassword, "WrongPassword123!"),
                                "Password verification should fail with incorrect password");

                assertFalse(Cryptography.verify(hashedPassword, TEST_PASSWORD + "a"),
                                "Password verification should fail with modified password");
        }

        @Test
        @DisplayName("Test encryption and decryption with default project key")
        public void testDefaultEncryptionDecryption() {
                String encrypted = Cryptography.encrypt(TEST_TEXT);
                assertNotNull(encrypted, "Encrypted text should not be null");
                assertNotEquals(TEST_TEXT, encrypted, "Encrypted text should differ from plaintext");

                String decrypted = Cryptography.decrypt(encrypted);
                assertEquals(TEST_TEXT, decrypted, "Decrypted text should match original plaintext");
        }

        @Test
        @DisplayName("Test encryption and decryption with custom key")
        public void testCustomKeyEncryptionDecryption() {
                String derivedKey = Cryptography.deriveKey(TEST_PASSWORD, TEST_SALT);

                String encrypted = Cryptography.encrypt(TEST_TEXT, derivedKey);
                assertNotNull(encrypted, "Encrypted text should not be null");
                assertNotEquals(TEST_TEXT, encrypted, "Encrypted text should differ from plaintext");

                String decrypted = Cryptography.decrypt(encrypted, derivedKey);
                assertEquals(TEST_TEXT, decrypted, "Decrypted text should match original plaintext");

                String wrongKey = Cryptography.deriveKey("WrongPassword123!", TEST_SALT);
                assertThrows(JoltSecurityException.class, () -> {
                        Cryptography.decrypt(encrypted, wrongKey);
                }, "Decryption with wrong key should throw JoltSecurityException");
        }

        @Test
        @DisplayName("Test key derivation")
        public void testKeyDerivation() {
                String key1 = Cryptography.deriveKey(TEST_PASSWORD, TEST_SALT);
                assertNotNull(key1, "Derived key should not be null");

                String key2 = Cryptography.deriveKey(TEST_PASSWORD, TEST_SALT);
                assertEquals(key1, key2, "Same password and salt should produce the same key");

                String key3 = Cryptography.deriveKey(TEST_PASSWORD, "DifferentSalt");
                assertNotEquals(key1, key3, "Different salt should produce a different key");

                String key4 = Cryptography.deriveKey("DifferentPassword123!", TEST_SALT);
                assertNotEquals(key1, key4, "Different password should produce a different key");
        }

        @Test
        @DisplayName("Test random password generation")
        public void testRandomPasswordGeneration() {
                String password1 = Cryptography.generateRandomPassword(8);
                assertEquals(8, password1.length(), "Generated password should have specified length");
                assertTrue(Cryptography.isStrongPassword(password1), "Generated password should be strong");

                String password2 = Cryptography.generateRandomPassword(16);
                assertEquals(16, password2.length(), "Generated password should have specified length");
                assertTrue(Cryptography.isStrongPassword(password2), "Generated password should be strong");

                String password3 = Cryptography.generateRandomPassword(12);
                String password4 = Cryptography.generateRandomPassword(12);
                assertNotEquals(password3, password4, "Different calls should generate different passwords");
        }

        @Test
        @DisplayName("Test password strength validation")
        public void testPasswordStrengthValidation() {
                assertTrue(Cryptography.isStrongPassword("Abcd1234!"),
                                "Password with uppercase, lowercase, digit, and special char should be strong");
                assertTrue(Cryptography.isStrongPassword("P@ssw0rd"),
                                "Password with uppercase, lowercase, digit, and special char should be strong");
                assertTrue(Cryptography.isStrongPassword("ComplexP@ssw0rd123"), "Complex password should be strong");

                assertFalse(Cryptography.isStrongPassword("abc123"), "Short password should not be strong");
                assertFalse(Cryptography.isStrongPassword("password"),
                                "Password without uppercase, digit, or special char should not be strong");
                assertFalse(Cryptography.isStrongPassword("PASSWORD123"),
                                "Password without lowercase or special char should not be strong");
                assertFalse(Cryptography.isStrongPassword("Password"),
                                "Password without digit or special char should not be strong");
                assertFalse(Cryptography.isStrongPassword("password123"),
                                "Password without uppercase or special char should not be strong");
                assertFalse(Cryptography.isStrongPassword("Password123"),
                                "Password without special char should not be strong");
                assertFalse(Cryptography.isStrongPassword(null), "Null password should not be strong");
        }

        @Test
        @DisplayName("Test multiple encryptions of same plaintext produce different ciphertexts")
        public void testEncryptionRandomness() {
                String encrypted1 = Cryptography.encrypt(TEST_TEXT);
                String encrypted2 = Cryptography.encrypt(TEST_TEXT);
                String encrypted3 = Cryptography.encrypt(TEST_TEXT);

                assertNotEquals(encrypted1, encrypted2, "Multiple encryptions should produce different ciphertexts");
                assertNotEquals(encrypted1, encrypted3, "Multiple encryptions should produce different ciphertexts");
                assertNotEquals(encrypted2, encrypted3, "Multiple encryptions should produce different ciphertexts");

                assertEquals(TEST_TEXT, Cryptography.decrypt(encrypted1),
                                "All ciphertexts should decrypt to original plaintext");
                assertEquals(TEST_TEXT, Cryptography.decrypt(encrypted2),
                                "All ciphertexts should decrypt to original plaintext");
                assertEquals(TEST_TEXT, Cryptography.decrypt(encrypted3),
                                "All ciphertexts should decrypt to original plaintext");
        }
}