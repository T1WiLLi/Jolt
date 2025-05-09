package io.github.t1willi.security.authentification;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.security.session.Session;

public final class SessionAuthStrategy implements AuthStrategy {

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
