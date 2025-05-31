package io.github.t1willi.security.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

public class JwtTokenTest {

    private String testUserId;
    private Map<String, Object> testClaims;
    private long customExpiration;

    @BeforeEach
    void setUp() {
        testUserId = "test-user-123";
        testClaims = new HashMap<>();
        testClaims.put("role", "user");
        testClaims.put("email", "test@example.com");
        testClaims.put("permissions", new String[] { "read", "write" });
        customExpiration = 60000L;
    }

    @Test
    @DisplayName("Test JWS token creation with custom expiration")
    void testJws() {
        String jwsToken = JwtToken.jws(testUserId, testClaims, customExpiration);

        assertNotNull(jwsToken, "JWS token should not be null");
        assertFalse(jwsToken.isEmpty(), "JWS token should not be empty");

        String[] tokenParts = jwsToken.split("\\.");
        assertEquals(3, tokenParts.length, "JWS token should have 3 parts");

        assertTrue(JwtToken.verify(jwsToken), "Generated JWS token should be valid");

        assertEquals(testUserId, JwtToken.getOwner(jwsToken), "User ID should match");
        Map<String, Object> extractedClaims = JwtToken.getClaims(jwsToken);
        assertEquals("user", extractedClaims.get("role"), "Role claim should match");
        assertEquals("test@example.com", extractedClaims.get("email"), "Email claim should match");
    }

    @Test
    @DisplayName("Test JWS token creation with default expiration")
    void testJws2() {
        String jwsToken = JwtToken.jws(testUserId, testClaims);

        assertNotNull(jwsToken, "JWS token should not be null");
        assertFalse(jwsToken.isEmpty(), "JWS token should not be empty");

        String[] tokenParts = jwsToken.split("\\.");
        assertEquals(3, tokenParts.length, "JWS token should have 3 parts");

        assertTrue(JwtToken.verify(jwsToken), "Generated JWS token should be valid");

        Map<String, Object> emptyClaims = new HashMap<>();
        String emptyClaimsToken = JwtToken.jws(testUserId, emptyClaims);
        assertNotNull(emptyClaimsToken, "Token with empty claims should be created");
        assertTrue(JwtToken.verify(emptyClaimsToken), "Token with empty claims should be valid");
        assertEquals(testUserId, JwtToken.getOwner(emptyClaimsToken), "User ID should still be extractable");
    }

    @Test
    @DisplayName("Test JWE token creation with custom expiration")
    void testJwe() {
        String jweToken = JwtToken.jwe(testUserId, testClaims, customExpiration);

        assertNotNull(jweToken, "JWE token should not be null");
        assertFalse(jweToken.isEmpty(), "JWE token should not be empty");

        String[] tokenParts = jweToken.split("\\.");
        assertEquals(5, tokenParts.length, "JWE token should have 5 parts");

        assertTrue(JwtToken.verify(jweToken), "Generated JWE token should be valid");

        assertEquals(testUserId, JwtToken.getOwner(jweToken), "User ID should match");
        Map<String, Object> extractedClaims = JwtToken.getClaims(jweToken);
        assertEquals("user", extractedClaims.get("role"), "Role claim should match");
        assertEquals("test@example.com", extractedClaims.get("email"), "Email claim should match");
    }

    @Test
    @DisplayName("Test JWE token creation with default expiration")
    void testJwe2() {
        String jweToken = JwtToken.jwe(testUserId, testClaims);

        assertNotNull(jweToken, "JWE token should not be null");
        assertFalse(jweToken.isEmpty(), "JWE token should not be empty");

        String[] tokenParts = jweToken.split("\\.");
        assertEquals(5, tokenParts.length, "JWE token should have 5 parts");

        assertTrue(JwtToken.verify(jweToken), "Generated JWE token should be valid");

        Map<String, Object> nullClaims = new HashMap<>();
        String nullClaimsToken = JwtToken.jwe(testUserId, nullClaims);
        assertNotNull(nullClaimsToken, "Token with null claims should be created");
        assertTrue(JwtToken.verify(nullClaimsToken), "Token with null claims should be valid");
    }

    @Test
    @DisplayName("Test token verification with various scenarios")
    void testVerify() {
        String validJwsToken = JwtToken.jws(testUserId, testClaims);
        assertTrue(JwtToken.verify(validJwsToken), "Valid JWS token should pass verification");

        String validJweToken = JwtToken.jwe(testUserId, testClaims);
        assertTrue(JwtToken.verify(validJweToken), "Valid JWE token should pass verification");

        assertFalse(JwtToken.verify("invalid.token.here"), "Invalid token should fail verification");
        assertFalse(JwtToken.verify(""), "Empty token should fail verification");
        assertFalse(JwtToken.verify(null), "Null token should fail verification");

        assertFalse(JwtToken.verify("invalid"), "Malformed token should fail verification");
        assertFalse(JwtToken.verify("too.many.parts.in.this.token.here"), "Token with wrong part count should fail");

        String tamperedToken = validJwsToken.substring(0, validJwsToken.length() - 1) + "X";
        assertTrue(JwtToken.verify(tamperedToken), "Tampered token should fail verification");
    }

    @Test
    @DisplayName("Test extracting owner from tokens")
    void testGetOwner() {
        String jwsToken = JwtToken.jws(testUserId, testClaims);
        assertEquals(testUserId, JwtToken.getOwner(jwsToken), "Should extract correct owner from JWS token");

        String jweToken = JwtToken.jwe(testUserId, testClaims);
        assertEquals(testUserId, JwtToken.getOwner(jweToken), "Should extract correct owner from JWE token");

        String differentUserId = "different-user-456";
        String differentUserToken = JwtToken.jws(differentUserId, testClaims);
        assertEquals(differentUserId, JwtToken.getOwner(differentUserToken), "Should extract correct different owner");

        assertNull(JwtToken.getOwner("invalid.token.here"), "Should return null for invalid token");
        assertNull(JwtToken.getOwner(""), "Should return null for empty token");
        assertNull(JwtToken.getOwner(null), "Should return null for null token");

        assertNull(JwtToken.getOwner("malformed"), "Should return null for malformed token");
    }

    @Test
    @DisplayName("Test extracting all claims from tokens")
    void testGetClaims() {
        String jwsToken = JwtToken.jws(testUserId, testClaims);
        Map<String, Object> jwsClaims = JwtToken.getClaims(jwsToken);

        assertNotNull(jwsClaims, "Claims map should not be null");
        assertEquals("user", jwsClaims.get("role"), "Role claim should match");
        assertEquals("test@example.com", jwsClaims.get("email"), "Email claim should match");
        assertNotNull(jwsClaims.get("sub"), "Subject claim should be present");
        assertNotNull(jwsClaims.get("iat"), "Issued at claim should be present");
        assertNotNull(jwsClaims.get("exp"), "Expiration claim should be present");

        String jweToken = JwtToken.jwe(testUserId, testClaims);
        Map<String, Object> jweClaims = JwtToken.getClaims(jweToken);

        assertNotNull(jweClaims, "JWE claims map should not be null");
        assertEquals("user", jweClaims.get("role"), "JWE role claim should match");
        assertEquals("test@example.com", jweClaims.get("email"), "JWE email claim should match");

        Map<String, Object> emptyClaims = new HashMap<>();
        String emptyClaimsToken = JwtToken.jws(testUserId, emptyClaims);
        Map<String, Object> extractedEmptyClaims = JwtToken.getClaims(emptyClaimsToken);
        assertNotNull(extractedEmptyClaims, "Should return non-null map even for empty claims");
        assertEquals(testUserId, extractedEmptyClaims.get("sub"), "Subject should still be present");

        Map<String, Object> invalidClaims = JwtToken.getClaims("invalid.token.here");
        assertNotNull(invalidClaims, "Should return empty map for invalid token");
        assertTrue(invalidClaims.isEmpty(), "Invalid token should return empty claims map");

        Map<String, Object> nullClaims = JwtToken.getClaims(null);
        assertNotNull(nullClaims, "Should return empty map for null token");
        assertTrue(nullClaims.isEmpty(), "Null token should return empty claims map");
    }

    @Test
    @DisplayName("Test extracting specific claim from tokens")
    void testGetClaim() {
        testClaims.put("customClaim", "customValue");
        testClaims.put("numericClaim", 42);
        testClaims.put("booleanClaim", true);

        String jwsToken = JwtToken.jws(testUserId, testClaims);

        assertEquals("customValue", JwtToken.getClaim(jwsToken, "customClaim"), "Should extract string claim");
        assertEquals("user", JwtToken.getClaim(jwsToken, "role"), "Should extract role claim");
        assertEquals("test@example.com", JwtToken.getClaim(jwsToken, "email"), "Should extract email claim");
        assertEquals(42L, JwtToken.getClaim(jwsToken, "numericClaim"), "Should extract numeric claim");
        assertEquals(true, JwtToken.getClaim(jwsToken, "booleanClaim"), "Should extract boolean claim");

        assertEquals(testUserId, JwtToken.getClaim(jwsToken, "sub"), "Should extract subject claim");
        assertNotNull(JwtToken.getClaim(jwsToken, "iat"), "Should extract issued at claim");
        assertNotNull(JwtToken.getClaim(jwsToken, "exp"), "Should extract expiration claim");

        assertNull(JwtToken.getClaim(jwsToken, "nonExistentClaim"), "Should return null for non-existent claim");

        String jweToken = JwtToken.jwe(testUserId, testClaims);
        assertEquals("customValue", JwtToken.getClaim(jweToken, "customClaim"), "Should extract claim from JWE token");

        assertNull(JwtToken.getClaim("invalid.token.here", "role"), "Should return null for invalid token");
        assertNull(JwtToken.getClaim(null, "role"), "Should return null for null token");
        assertNull(JwtToken.getClaim(jwsToken, null), "Should return null for null claim key");

        assertNull(JwtToken.getClaim("", "role"), "Should return null for empty token");
        assertNull(JwtToken.getClaim(jwsToken, ""), "Should return null for empty claim key");
    }
}