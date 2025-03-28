package io.github.t1willi.security.utils;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;

import io.github.t1willi.security.cryptography.Cryptography;
import io.github.t1willi.server.config.ConfigurationManager;

import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.spec.SecretKeySpec;

/**
 * Comprehensive utility class for creating and verifying JWT (JSON Web Tokens)
 * with support for both JWS (signed) and JWE (encrypted) tokens using Nimbus
 * JOSE + JWT.
 * 
 * @author William Beaudin
 * @version 2.1
 * @since 2025-03-28
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
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(userID)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + expirationMs));

            claims.forEach(claimsBuilder::claim);

            JWTClaimsSet jwtClaims = claimsBuilder.build();
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.HS256)
                    .type(JOSEObjectType.JWT)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, jwtClaims);
            signedJWT.sign(new MACSigner(getSigningKey()));

            return signedJWT.serialize();
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
            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(userID)
                    .issueTime(new Date())
                    .expirationTime(new Date(System.currentTimeMillis() + expirationMs));

            claims.forEach(claimsBuilder::claim);

            JWTClaimsSet jwtClaims = claimsBuilder.build();

            JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.A256KW, EncryptionMethod.A256GCM)
                    .type(JOSEObjectType.JWT)
                    .build();

            EncryptedJWT encryptedJWT = new EncryptedJWT(header, jwtClaims);
            encryptedJWT.encrypt(new AESEncrypter(getEncryptionKey()));

            return encryptedJWT.serialize();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to create JWE token", e);
            throw new RuntimeException("Failed to create JWE token", e);
        }
    }

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
            if (token.split("\\.").length == 3) {
                SignedJWT signedJWT = SignedJWT.parse(token);
                signedJWT.verify(new MACVerifier(getSigningKey()));
            } else {
                EncryptedJWT encryptedJWT = EncryptedJWT.parse(token);
                encryptedJWT.decrypt(new AESDecrypter(getEncryptionKey()));
            }
            return true;
        } catch (Exception e) {
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
            JWTClaimsSet claims = parseToken(token);
            return claims.getSubject();
        } catch (Exception e) {
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
            JWTClaimsSet claims = parseToken(token);
            return new HashMap<>(claims.getClaims());
        } catch (Exception e) {
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
     * Parse and verify the JWT token based on its type.
     *
     * @param token JWT token
     * @return Parsed claims set
     * @throws ParseException if token parsing fails
     * @throws JOSEException  if token verification fails
     */
    private static JWTClaimsSet parseToken(String token) throws ParseException, JOSEException {
        if (token.split("\\.").length == 3) {
            SignedJWT signedJWT = SignedJWT.parse(token);
            signedJWT.verify(new MACVerifier(getSigningKey()));
            return signedJWT.getJWTClaimsSet();
        } else {
            EncryptedJWT encryptedJWT = EncryptedJWT.parse(token);
            encryptedJWT.decrypt(new AESDecrypter(getEncryptionKey()));
            return encryptedJWT.getJWTClaimsSet();
        }
    }

    /**
     * Creates a secure signing key using secret key and pepper.
     *
     * @return Signing key for JWT
     */
    private static SecretKeySpec getSigningKey() {
        String combined = SECRET_KEY + PEPPER;
        byte[] keyBytes = combined.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new SecretKeySpec(Arrays.copyOf(keyBytes, 32), "HmacSHA256");
    }

    /**
     * Creates an encryption key for JWT.
     *
     * @return Encryption key for JWT
     */
    private static SecretKeySpec getEncryptionKey() {
        byte[] keyBytes = ENCRYPTION_KEY.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new SecretKeySpec(Arrays.copyOf(keyBytes, 32), "AES");
    }
}