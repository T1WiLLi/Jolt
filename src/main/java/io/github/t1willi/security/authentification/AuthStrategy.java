package io.github.t1willi.security.authentification;

import io.github.t1willi.context.JoltContext;

/**
 * Authentification strategy interface for defining how a user should be
 * authenticated.
 * This interface can be implemented by various authentication strategies such
 * as
 * Basic Authentication, OAuth2, JWT, etc.
 * 
 * By default, The authentication system behind Jolt is Session-based, which
 * is the default authentication strategy.
 * 
 * @version 2.6.2
 * @since 2025-03-28
 * @author William Beaudin
 */
public interface AuthStrategy {

    /**
     * Check whether the current request context is authenticated.
     * 
     * @param context Current request context.
     * @return true if the user is authenticated, false otherwise.
     */
    boolean authenticate(JoltContext context);

    /**
     * Challenge the client when authentication fails (e.g., set status or headers).
     * 
     * @param context Current request context.
     */
    void challenge(JoltContext context);
}
