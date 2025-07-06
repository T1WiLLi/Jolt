package io.github.t1willi.security.authentification;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.security.utils.JwtToken;

/**
 * JWT authentication strategy for Jolt applications.
 * This strategy allows you to set criteria for JWT claims that must be
 * present and match expected values for successful authentication.
 * 
 * It checks the "Authorization" header for a Bearer token,
 * decodes the JWT, and verifies that the claims match the specified criteria.
 * If the claims are valid, it allows access;
 * otherwise, it challenges the client with a 401 Unauthorized response.
 */
public final class JWTAuthStrategy implements AuthStrategy {
    private static final Map<String, Object> criteria = Collections.synchronizedMap(new HashMap<>());

    public static void criteria(String key, Object expectedValue) {
        JWTAuthStrategy.criteria.put(key, expectedValue);
    }

    public static void criteria(Map<String, Object> criteria) {
        JWTAuthStrategy.criteria.putAll(criteria);
    }

    public void addCriteria(String key, Object expectedValue) {
        criteria.put(key, expectedValue);
    }

    public void addCriteria(Map<String, Object> newCriteria) {
        criteria.putAll(newCriteria);
    }

    @Override
    public boolean authenticate(JoltContext context) {
        Optional<String> authHeader = context.bearerToken();
        if (!authHeader.isPresent()) {
            return false;
        }

        String token = authHeader.get();
        try {
            Map<String, Object> claims = JwtToken.getClaims(token);
            if (claims == null) {
                return false;
            }

            synchronized (criteria) {
                for (Map.Entry<String, Object> entry : criteria.entrySet()) {
                    String key = entry.getKey();
                    Object expectedValue = entry.getValue();
                    Object actualValue = claims.get(key);
                    if (actualValue == null || !actualValue.equals(expectedValue)) {
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void challenge(JoltContext context) {
        context
                .header("WWW-Authenticate", "Bearer realm=\"Restricted Area\"")
                .status(401)
                .contentType("application/x-www-form-urlencoded");
    }
}
