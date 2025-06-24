package io.github.t1willi.security.authentification;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import io.github.t1willi.context.JoltContext;

/**
 * Basic authentication strategy for Jolt applications.
 * This strategy allows you to set credentials either
 * programmatically or via a supplier.
 * 
 * It checks the "Authorization" header for a Basic
 * authentication scheme and validates the provided
 * username and password against the configured credentials.
 * If the credentials are valid, it allows access;
 * otherwise, it challenges the client with a 401 Unauthorized response.
 */
public final class BasicAuthStrategy implements AuthStrategy {

    private static Supplier<Map<String, String>> credentialsSupplier = HashMap::new;

    public static void credentials(String username, String password) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put(username, password);
        credentialsSupplier = () -> credentials;
    }

    public static void credentials(Map<String, String> credentials) {
        credentialsSupplier = () -> credentials;
    }

    public static void credentials(Supplier<Map<String, String>> credentialsSupplier) {
        BasicAuthStrategy.credentialsSupplier = credentialsSupplier;
    }

    @Override
    public boolean authenticate(JoltContext context) {
        String authHeader = context.rawRequest().getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Basic ")) {
            return false;
        }

        String base64Credentials = authHeader.substring("Basic ".length()).trim();
        String credentials;
        try {
            credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return false;
        }

        String[] parts = credentials.split(":", 2);
        if (parts.length != 2) {
            return false;
        }

        String username = parts[0];
        String password = parts[1];

        Map<String, String> credentialsMap = credentialsSupplier.get();
        return credentialsMap.containsKey(username) && credentialsMap.get(username).equals(password);
    }

    @Override
    public void challenge(JoltContext context) {
        context
                .header("WWW-Authenticate", "Basic realm=\"Restricted Area\"")
                .status(401)
                .contentType("application/x-www-form-urlencoded");
    }
}
