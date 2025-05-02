package io.github.t1willi.cryptography;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.t1willi.security.cryptography.CryptographyUtils;

import java.util.Base64;
import java.util.regex.Pattern;

/**
 * Test suite for the CryptographyUtils class.
 * Tests all methods for generating random strings, bytes, and Base64-encoded
 * values,
 * ensuring correct length, character sets, and exception handling.
 */
public class CryptographyUtilsTests {

        private static final String HEX_PATTERN = "^[0-9A-F]+$";
        private static final String ALPHANUMERIC_PATTERN = "^[0-9A-Za-z]+$";
        private static final String BASE64_PATTERN = "^[A-Za-z0-9+/=]+$";
        private static final String URL_SAFE_BASE64_PATTERN = "^[A-Za-z0-9_-]+$";

        @Test
        @DisplayName("Test randomHex generates correct length and hexadecimal characters")
        public void testRandomHex() {
                String hex = CryptographyUtils.randomHex(16);
                assertEquals(16, hex.length(), "Hex string should have specified length");
                assertTrue(Pattern.matches(HEX_PATTERN, hex), "Hex string should contain only 0-9, A-F");

                hex = CryptographyUtils.randomHex(8);
                assertEquals(8, hex.length(), "Hex string should have specified length");
                assertTrue(Pattern.matches(HEX_PATTERN, hex), "Hex string should contain only 0-9, A-F");
        }

        @Test
        @DisplayName("Test randomHex throws exception for invalid length")
        public void testRandomHexInvalidLength() {
                assertThrows(IllegalArgumentException.class, () -> CryptographyUtils.randomHex(0),
                                "Should throw IllegalArgumentException for zero length");
                assertThrows(IllegalArgumentException.class, () -> CryptographyUtils.randomHex(-1),
                                "Should throw IllegalArgumentException for negative length");
        }

        @Test
        @DisplayName("Test randomString generates correct length and uses provided character set")
        public void testRandomString() {
                String chars = "ABC";
                String result = CryptographyUtils.randomString(10, chars);
                assertEquals(10, result.length(), "Random string should have specified length");
                assertTrue(result.matches("^[ABC]+$"),
                                "Random string should only contain characters from provided set");

                chars = "12345";
                result = CryptographyUtils.randomString(5, chars);
                assertEquals(5, result.length(), "Random string should have specified length");
                assertTrue(result.matches("^[1-5]+$"),
                                "Random string should only contain characters from provided set");
        }

        @Test
        @DisplayName("Test randomString throws exception for invalid inputs")
        public void testRandomStringInvalidInputs() {
                assertThrows(IllegalArgumentException.class, () -> CryptographyUtils.randomString(0, "ABC"),
                                "Should throw IllegalArgumentException for zero length");
                assertThrows(IllegalArgumentException.class, () -> CryptographyUtils.randomString(-1, "ABC"),
                                "Should throw IllegalArgumentException for negative length");
                assertThrows(IllegalArgumentException.class, () -> CryptographyUtils.randomString(10, null),
                                "Should throw IllegalArgumentException for null character set");
                assertThrows(IllegalArgumentException.class, () -> CryptographyUtils.randomString(10, ""),
                                "Should throw IllegalArgumentException for empty character set");
        }

        @Test
        @DisplayName("Test randomAlphanumeric generates correct length and alphanumeric characters")
        public void testRandomAlphanumeric() {
                String alphanumeric = CryptographyUtils.randomAlphanumeric(20);
                assertEquals(20, alphanumeric.length(), "Alphanumeric string should have specified length");
                assertTrue(Pattern.matches(ALPHANUMERIC_PATTERN, alphanumeric),
                                "Alphanumeric string should contain only 0-9, A-Z, a-z");

                alphanumeric = CryptographyUtils.randomAlphanumeric(8);
                assertEquals(8, alphanumeric.length(), "Alphanumeric string should have specified length");
                assertTrue(Pattern.matches(ALPHANUMERIC_PATTERN, alphanumeric),
                                "Alphanumeric string should contain only 0-9, A-Z, a-z");
        }

        @Test
        @DisplayName("Test randomAlphanumeric throws exception for invalid length")
        public void testRandomAlphanumericInvalidLength() {
                assertThrows(IllegalArgumentException.class, () -> CryptographyUtils.randomAlphanumeric(0),
                                "Should throw IllegalArgumentException for zero length");
                assertThrows(IllegalArgumentException.class, () -> CryptographyUtils.randomAlphanumeric(-1),
                                "Should throw IllegalArgumentException for negative length");
        }

        @Test
        @DisplayName("Test randomBytes generates correct length")
        public void testRandomBytes() {
                byte[] bytes = CryptographyUtils.randomBytes(16);
                assertEquals(16, bytes.length, "Byte array should have specified length");
                assertNotNull(bytes, "Byte array should not be null");

                bytes = CryptographyUtils.randomBytes(32);
                assertEquals(32, bytes.length, "Byte array should have specified length");
                assertNotNull(bytes, "Byte array should not be null");
        }

        @Test
        @DisplayName("Test randomBase64 generates valid Base64 string")
        public void testRandomBase64() {
                String base64 = CryptographyUtils.randomBase64(16);
                assertTrue(Pattern.matches(BASE64_PATTERN, base64),
                                "Base64 string should contain valid Base64 characters");
                byte[] decoded = Base64.getDecoder().decode(base64);
                assertEquals(16, decoded.length, "Decoded Base64 string should match input byte length");

                base64 = CryptographyUtils.randomBase64(32);
                assertTrue(Pattern.matches(BASE64_PATTERN, base64),
                                "Base64 string should contain valid Base64 characters");
                decoded = Base64.getDecoder().decode(base64);
                assertEquals(32, decoded.length, "Decoded Base64 string should match input byte length");
        }

        @Test
        @DisplayName("Test randomUrlSafeBase64 generates valid URL-safe Base64 string")
        public void testRandomUrlSafeBase64() {
                String urlSafeBase64 = CryptographyUtils.randomUrlSafeBase64(16);
                assertTrue(Pattern.matches(URL_SAFE_BASE64_PATTERN, urlSafeBase64),
                                "URL-safe Base64 string should contain only A-Z, a-z, 0-9, _, -");
                byte[] decoded = Base64.getUrlDecoder().decode(urlSafeBase64);
                assertEquals(16, decoded.length, "Decoded URL-safe Base64 string should match input byte length");

                urlSafeBase64 = CryptographyUtils.randomUrlSafeBase64(32);
                assertTrue(Pattern.matches(URL_SAFE_BASE64_PATTERN, urlSafeBase64),
                                "URL-safe Base64 string should contain only A-Z, a-z, 0-9, _, -");
                decoded = Base64.getUrlDecoder().decode(urlSafeBase64);
                assertEquals(32, decoded.length, "Decoded URL-safe Base64 string should match input byte length");
        }
}