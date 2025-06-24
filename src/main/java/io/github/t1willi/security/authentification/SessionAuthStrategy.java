package io.github.t1willi.security.authentification;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.security.session.Session;

public final class SessionAuthStrategy implements AuthStrategy {

    public static Map<String, Object> getSessionAttributes(Set<String> keys) {
        if (!Session.isAuthenticated()) {
            return null;
        }
        Map<String, Object> attributes = new HashMap<>();
        for (String key : keys) {
            Session.getOptional(key).ifPresent(value -> attributes.put(key, value));
        }
        return attributes;
    }

    @Override
    public boolean authenticate(JoltContext context) {
        return Session.isAuthenticated();
    }

    @Override
    public void challenge(JoltContext context) {
        context.header("WWW-Authenticate", "Session realm=\"Access to the protected resource\"")
                .abortUnauthorized("Authentication required");
    }
}
