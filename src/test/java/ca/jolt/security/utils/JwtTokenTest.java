package ca.jolt.security.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.t1willi.security.utils.JwtToken;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@ExtendWith(MockitoExtension.class)
public class JwtTokenTest {

    private static final String TEST_SECRET_KEY = "testSecretKeyWithAtLeast32Characters12345";
    private static final String TEST_PEPPER = "testPepperWithAtLeast32Characters123456789";
    private static final String TEST_USER_ID = "user123";

    @BeforeEach
    public void setUp() throws Exception {

    }

    @Test
    public void testCreate_WithValidInput_ReturnsToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ADMIN");
        String token = JwtToken.create(TEST_USER_ID, claims);
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    public void testCreate_WithCustomExpiration_ReturnsToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        long expirationMs = 3600000; // 1 hour
        String token = JwtToken.create(TEST_USER_ID, claims, expirationMs);
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    public void testVerify_WithValidToken_ReturnsTrue() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ADMIN");
        String token = JwtToken.create(TEST_USER_ID, claims);
        boolean isValid = JwtToken.verify(token);
        assertTrue(isValid);
    }

    @Test
    public void testVerify_WithInvalidToken_ReturnsFalse() {
        String invalidToken = "invalid.token.string";
        boolean isValid = JwtToken.verify(invalidToken);
        assertFalse(isValid);
    }

    @Test
    public void testVerify_WithExpiredToken_ReturnsFalse() throws Exception {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        SecretKey secretKey = Keys.hmacShaKeyFor((TEST_SECRET_KEY + TEST_PEPPER).getBytes(StandardCharsets.UTF_8));
        JwtBuilder builder = Jwts.builder()
                .setSubject(TEST_USER_ID)
                .addClaims(claims)
                .setIssuedAt(Date.from(Instant.now().minusSeconds(120))) // 2 minutes ago
                .setExpiration(Date.from(Instant.now().minusSeconds(60))) // expired 1 minute ago
                .signWith(secretKey, SignatureAlgorithm.HS512);

        String expiredToken = builder.compact();
        boolean isValid = JwtToken.verify(expiredToken);
        assertFalse(isValid);
    }

    @Test
    public void testGetOwner_WithValidToken_ReturnsUserId() {
        Map<String, Object> claims = new HashMap<>();
        String token = JwtToken.create(TEST_USER_ID, claims);
        String userId = JwtToken.getOwner(token);
        assertEquals(TEST_USER_ID, userId);
    }

    @Test
    public void testGetOwner_WithInvalidToken_ReturnsNull() {
        String invalidToken = "invalid.token.string";
        String userId = JwtToken.getOwner(invalidToken);
        assertNull(userId);
    }

    @Test
    public void testGetClaims_WithValidToken_ReturnsClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "ADMIN");
        claims.put("permission", "READ_WRITE");
        String token = JwtToken.create(TEST_USER_ID, claims);
        Claims retrievedClaims = JwtToken.getClaims(token);
        assertNotNull(retrievedClaims);
        assertEquals(TEST_USER_ID, retrievedClaims.getSubject());
        assertEquals("ADMIN", retrievedClaims.get("role"));
        assertEquals("READ_WRITE", retrievedClaims.get("permission"));
    }

    @Test
    public void testGetClaims_WithInvalidToken_ReturnsNull() {
        String invalidToken = "invalid.token.string";
        Claims claims = JwtToken.getClaims(invalidToken);
        assertNull(claims);
    }

    @Test
    public void testGetClaim_WithValidTokenAndKey_ReturnsClaimValue() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "MANAGER");
        String token = JwtToken.create(TEST_USER_ID, claims);
        Object roleValue = JwtToken.getClaim(token, "role");
        assertEquals("MANAGER", roleValue);
    }

    @Test
    public void testGetClaim_WithValidTokenAndInvalidKey_ReturnsNull() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", "USER");
        String token = JwtToken.create(TEST_USER_ID, claims);
        Object nonExistentValue = JwtToken.getClaim(token, "nonexistent");
        assertNull(nonExistentValue);
    }

    @Test
    public void testGetClaim_WithInvalidToken_ReturnsNull() {
        String invalidToken = "invalid.token.string";
        Object value = JwtToken.getClaim(invalidToken, "role");
        assertNull(value);
    }
}