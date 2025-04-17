package io.github.t1willi.security.session;

import io.github.t1willi.security.cryptography.Cryptography;
import io.github.t1willi.server.config.ConfigurationManager;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Wraps a standard {@link HttpSession} to provide:
 * <ul>
 * <li>Optional AES‑GCM encryption for String values</li>
 * <li>Type‑safe {@link Optional} getters</li>
 * <li>Session‑ID rotation for fixation protection</li>
 * </ul>
 *
 * <p>
 * When {@code session.encrypt=true}, all stored Strings (except
 * core keys) are encrypted with the app’s master key and
 * transparently decrypted on read.
 * </p>
 *
 * @see Session
 * @see Cryptography
 */
public final class JoltSession {
    private static final boolean ENCRYPT = Boolean.parseBoolean(
            ConfigurationManager.getInstance()
                    .getProperty("session.encrypt", "false"));

    private static final Set<String> NO_ENCRYPT = new HashSet<>(Arrays.asList(
            Session.KEY_INITIALIZED,
            Session.KEY_IP_ADDRESS,
            Session.KEY_USER_AGENT,
            Session.KEY_ACCESS_TIME,
            Session.KEY_EXPIRE_TIME,
            Session.KEY_IS_AUTHENTICATED));

    private final HttpSession session;

    /**
     * Wraps an existing {@link HttpSession}.
     *
     * @param session the raw HttpSession; must not be null
     * @throws NullPointerException if {@code session} is null
     */
    public JoltSession(HttpSession session) {
        if (session == null)
            throw new NullPointerException("HttpSession cannot be null");
        this.session = session;
    }

    /**
     * Stores an attribute under {@code key}. If encryption is enabled and
     * {@code value} is a String (and not in NO_ENCRYPT), it is encrypted first.
     *
     * @param key   the attribute name
     * @param value the attribute value
     */
    public void setAttribute(String key, Object value) {
        Object toStore = value;
        if (ENCRYPT && value instanceof String str && !NO_ENCRYPT.contains(key)) {
            toStore = Cryptography.encrypt(str);
        }
        session.setAttribute(key, toStore);
    }

    /**
     * Retrieves the attribute under {@code key}, decrypted if needed.
     *
     * @param key the attribute name
     * @return an Optional containing the stored String, or empty
     */
    public Optional<String> getAttribute(String key) {
        Object raw = session.getAttribute(key);
        if (raw == null)
            return Optional.empty();

        if (ENCRYPT && raw instanceof String enc && !NO_ENCRYPT.contains(key)) {
            return Optional.of(Cryptography.decrypt(enc));
        }
        return Optional.of(String.valueOf(raw));
    }

    /**
     * Removes the attribute under {@code key}.
     *
     * @param key the attribute name
     */
    public void removeAttribute(String key) {
        session.removeAttribute(key);
    }

    /** @return true if the session was just created in this request */
    public boolean isNew() {
        return session.isNew();
    }

    /** @return the raw session ID */
    public String getId() {
        return session.getId();
    }

    /**
     * Invalidates the wrapped session (all attributes removed).
     */
    public void invalidate() {
        session.invalidate();
    }

    /**
     * Exposes the raw {@link HttpSession}. Use sparingly.
     *
     * @return the underlying HttpSession
     */
    public HttpSession raw() {
        return session;
    }

    /**
     * Rotates the session ID (Servlet 3.1+). Preserves all attributes.
     *
     * @param request the current {@link HttpServletRequest}
     * @throws UnsupportedOperationException on older containers
     */
    public void changeSessionId(HttpServletRequest request) {
        request.changeSessionId();
    }
}
