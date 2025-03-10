package ca.jolt.security.utils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import ca.jolt.security.cryptography.Cryptography;
import ca.jolt.server.config.ConfigurationManager;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Utility class for creating and verifying JWS (signed) JWT tokens.
 * <p>
 * Configuration is read from:
 * <ul>
 * <li>server.jwt.secret_key</li>
 * <li>server.jwt.pepper</li>
 * </ul>
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * String token = JwtToken.create("user123", Map.of("role", "ADMIN"));
 * boolean valid = JwtToken.verify(token);
 * String userID = JwtToken.getOwner(token);
 * Map<String, Object> claims = JwtToken.getClaims(token);
 * }</pre>
 */
public final class JwtToken {

    private static final String SECRET_KEY;
    private static final String PEPPER;

    private static final long DEFAULT_EXPIRATION_MS = 1_800_000; // 30 minutes

    /**
     * Static block prepare the secret key and pepper.
     */
    static {
        SECRET_KEY = ConfigurationManager.getInstance().getProperty(
                "server.security.secret_key",
                Cryptography.randomBase64(32) // 256 bits
        );
        PEPPER = ConfigurationManager.getInstance().getProperty(
                "server.security.pepper",
                Cryptography.randomBase64(32) // 256 bits
        );
    }

    /**
     * Prevent instantiation.
     */
    private JwtToken() {
        // Prevent instantiation
    }

    /**
     * Generate a new JWT Token with the given owner and claims.
     * <p>
     * This implementation generate a JWS (signed) JWT token. And not a JWE
     * (encrypted) JWT token. It is signed with the Secret-key and pepper.
     * 
     * @param userID       the user ID
     * @param claims       the claims
     * @param expirationMs the expiration time in milliseconds
     * @return the JWT token
     */
    public static String create(String userID, Map<String, Object> claims, long expirationMs) {
        SecretKey secretKey = getSecretKey();

        JwtBuilder builder = Jwts.builder()
                .setSubject(userID)
                .addClaims(claims)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(Date.from(Instant.now().plusMillis(expirationMs)))
                .signWith(secretKey, SignatureAlgorithm.HS512);

        return builder.compact();
    }

    /**
     * A convenience method to generate a new JWT token with the given owner and
     * claims with the default expiration time (Same as cookie for JWT) of 30
     * minutes.
     * 
     * @param userID the user ID
     * @param claims the claims
     * @return the JWT token
     */
    public static String create(String userID, Map<String, Object> claims) {
        return create(userID, claims, DEFAULT_EXPIRATION_MS);
    }

    /**
     * A convenience method to generate a new JWT token with the given owner only.
     * 
     * @param userID the user ID
     * @return the JWT token
     */
    public static String create(String userID) {
        return create(userID, Collections.emptyMap(), DEFAULT_EXPIRATION_MS);
    }

    /**
     * Verify a JWT token expiration, signature and claims.
     * 
     * @param token the JWT token
     * @return True if the token is valid, false otherwise
     */
    public static boolean verify(String token) {
        try {
            SecretKey secretKey = getSecretKey();
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Get the subject of the JWT token.
     * 
     * @param token the JWT token
     * @return the subject (UserID).
     */
    public static String getOwner(String token) {
        if (verify(token)) {
            return getClaims(token).getSubject();
        }
        return null;
    }

    /**
     * Retrieves the claims from the JWT token.
     * 
     * @param token the JWT token
     * @return the claims
     */
    public static Claims getClaims(String token) {
        if (verify(token)) {
            try {
                SecretKey secretKey = getSecretKey();
                return Jwts.parserBuilder()
                        .setSigningKey(secretKey)
                        .build()
                        .parseClaimsJws(token).getBody();
            } catch (JwtException | IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Retrieve a claim from the JWT token.
     * 
     * @param token the JWT token
     * @param key   the claim key
     * @return the claim value
     */
    public static Object getClaim(String token, String key) {
        Claims claims = getClaims(token);
        return (claims != null) ? claims.get(key) : null;
    }

    private static SecretKey getSecretKey() {
        String combined = SECRET_KEY + PEPPER;
        return Keys.hmacShaKeyFor(combined.getBytes(StandardCharsets.UTF_8));
    }
}
