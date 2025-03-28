package io.github.t1willi.security.utils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jose4j.jwe.ContentEncryptionAlgorithmIdentifiers;
import org.jose4j.jwe.JsonWebEncryption;
import org.jose4j.jwe.KeyManagementAlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.AesKey;
import org.jose4j.keys.HmacKey;
import org.jose4j.lang.JoseException;

import io.github.t1willi.security.cryptography.Cryptography;
import io.github.t1willi.server.config.ConfigurationManager;

/**
 * Comprehensive utility class for creating and verifying JWT (JSON Web Tokens)
 * with support for both JWS (signed) and JWE (encrypted) tokens.
 * 
 * <p>
 * This class provides methods to generate, verify, and extract information
 * from JWT tokens using the jose4j library.
 * </p>
 * 
 * <p>
 * Configuration is dynamically retrieved from configuration properties:
 * </p>
 * <ul>
 * <li>server.security.secret_key</li>
 * <li>server.security.pepper</li>
 * <li>server.security.encryption_key</li>
 * </ul>
 * 
 * @author Your Name
 * @version 1.1
 * @since 2024-03-27
 */
public final class JwtToken {
    private static final Logger LOGGER = Logger.getLogger(JwtToken.class.getName());

    private static final String SECRET_KEY;
    private static final String PEPPER;
    private static final String ENCRYPTION_KEY;

    /** Default token expiration time: 30 minutes in milliseconds */
    private static final long DEFAULT_EXPIRATION_MS = 1_800_000;

    /**
     * Static initializer to load security keys from configuration.
     * Generates random keys if not explicitly configured.
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
        ENCRYPTION_KEY = ConfigurationManager.getInstance().getProperty(
                "server.security.encryption_key",
                Cryptography.randomBase64(32) // 256 bits
        );
    }

    /** Prevent instantiation of utility class */
    private JwtToken() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Generates a signed JWT (JWS) token with detailed claims and custom
     * expiration.
     *
     * @param userID       User identifier
     * @param claims       Additional claims to be included in the token
     * @param expirationMs Token expiration time in milliseconds
     * @return Signed JWT token as a string
     * @throws RuntimeException If token generation fails
     */
    public static String jws(String userID, Map<String, Object> claims, long expirationMs) {
        try {
            JwtClaims jwtClaims = new JwtClaims();
            jwtClaims.setSubject(userID);
            jwtClaims.setIssuedAt(NumericDate.fromSeconds(Instant.now().getEpochSecond()));
            jwtClaims.setExpirationTime(
                    NumericDate.fromSeconds(Instant.now().plusMillis(expirationMs).getEpochSecond()));

            for (Map.Entry<String, Object> entry : claims.entrySet()) {
                jwtClaims.setClaim(entry.getKey(), entry.getValue());
            }

            JsonWebSignature jws = new JsonWebSignature();
            jws.setPayload(jwtClaims.toJson());
            jws.setKey(getSigningKey());
            jws.setAlgorithmHeaderValue("HS512");
            jws.setHeader("typ", "JWT");
            return jws.getCompactSerialization();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create JWS token", e);
            throw new RuntimeException("Failed to create JWS token", e);
        }
    }

    /**
     * Generates an encrypted JWT (JWE) token with detailed claims and custom
     * expiration.
     *
     * @param userID       User identifier
     * @param claims       Additional claims to be included in the token
     * @param expirationMs Token expiration time in milliseconds
     * @return Encrypted JWT token as a string
     * @throws RuntimeException If token encryption fails
     */
    public static String jwe(String userID, Map<String, Object> claims, long expirationMs) {
        try {
            JwtClaims jwtClaims = new JwtClaims();
            jwtClaims.setSubject(userID);
            jwtClaims.setIssuedAt(NumericDate.fromSeconds(Instant.now().getEpochSecond()));
            jwtClaims.setExpirationTime(
                    NumericDate.fromSeconds(Instant.now().plusMillis(expirationMs).getEpochSecond()));

            for (Map.Entry<String, Object> entry : claims.entrySet()) {
                jwtClaims.setClaim(entry.getKey(), entry.getValue());
            }

            JsonWebEncryption jwe = new JsonWebEncryption();
            jwe.setPayload(jwtClaims.toJson());

            Key key = getEncryptionKey();

            jwe.setAlgorithmHeaderValue(KeyManagementAlgorithmIdentifiers.A256KW);
            jwe.setEncryptionMethodHeaderParameter(ContentEncryptionAlgorithmIdentifiers.AES_256_GCM);
            jwe.setKey(key);

            return jwe.getCompactSerialization();
        } catch (JoseException e) {
            LOGGER.log(Level.SEVERE, "Failed to create JWE token", e);
            throw new RuntimeException("Failed to create JWE token", e);
        }
    }

    // Convenience methods with default expiration
    public static String jws(String userID, Map<String, Object> claims) {
        return jws(userID, claims, DEFAULT_EXPIRATION_MS);
    }

    public static String jwe(String userID, Map<String, Object> claims) {
        return jwe(userID, claims, DEFAULT_EXPIRATION_MS);
    }

    /**
     * Verifies the authenticity and validity of a JWT token.
     *
     * @param token JWT token to verify
     * @return true if the token is valid, false otherwise
     */
    public static boolean verify(String token) {
        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setVerificationKey(getSigningKey())
                    .setDecryptionKey(getEncryptionKey())
                    .build();
            jwtConsumer.processToClaims(token);
            return true;
        } catch (InvalidJwtException e) {
            LOGGER.log(Level.INFO, "Token verification failed", e);
            return false;
        }
    }

    /**
     * Extracts the user ID (subject) from a valid JWT token.
     *
     * @param token JWT token
     * @return User ID or null if token is invalid
     */
    public static String getOwner(String token) {
        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setVerificationKey(getSigningKey())
                    .setDecryptionKey(getEncryptionKey())
                    .build();
            JwtClaims claims = jwtConsumer.processToClaims(token);
            return claims.getSubject();
        } catch (InvalidJwtException | MalformedClaimException e) {
            LOGGER.log(Level.INFO, "Could not extract owner from token", e);
            return null;
        }
    }

    /**
     * Retrieves all claims from a valid JWT token.
     *
     * @param token JWT token
     * @return Map of claims or null if token is invalid
     */
    public static Map<String, Object> getClaims(String token) {
        try {
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setVerificationKey(getSigningKey())
                    .setDecryptionKey(getEncryptionKey())
                    .build();
            JwtClaims claims = jwtConsumer.processToClaims(token);
            return new HashMap<>(claims.getClaimsMap());
        } catch (InvalidJwtException e) {
            LOGGER.log(Level.INFO, "Could not extract claims from token", e);
            return null;
        }
    }

    /**
     * Retrieves a specific claim from a valid JWT token.
     *
     * @param token JWT token
     * @param key   Claim key to retrieve
     * @return Claim value or null if token is invalid or claim not found
     */
    public static Object getClaim(String token, String key) {
        Map<String, Object> claims = getClaims(token);
        return claims != null ? claims.get(key) : null;
    }

    /**
     * Creates a secure signing key using secret key and pepper.
     *
     * @return Signing key for JWT
     */
    private static Key getSigningKey() {
        String combined = SECRET_KEY + PEPPER;
        return new HmacKey(combined.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Creates an encryption key for JWT.
     *
     * @return Encryption key for JWT
     */
    private static Key getEncryptionKey() {
        byte[] keyBytes = Arrays.copyOf(ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8), 32);
        return new AesKey(keyBytes);
    }
}