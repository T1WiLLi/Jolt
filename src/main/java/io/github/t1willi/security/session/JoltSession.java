// JoltSession.java
package io.github.t1willi.security.session;

import io.github.t1willi.security.cryptography.Cryptography;
import io.github.t1willi.server.config.ConfigurationManager;
import io.github.t1willi.utils.JacksonUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JavaType;

/**
 * Wraps a standard {@link HttpSession} to provide:
 * <ul>
 * <li>Optional AES‑GCM encryption for String values</li>
 * <li>Type‑safe {@link Optional} getters</li>
 * <li>Session‑ID rotation for fixation protection</li>
 * <li>Secure attribute cleanup</li>
 * </ul>
 *
 * <p>
 * When {@code session.encrypt=true}, all stored Strings (except
 * core keys) are encrypted with the app's master key and
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
            Session.KEY_LAST_ACCESS,
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
     * the attribute is eligible for encryption, it is serialized and encrypted
     * first.
     *
     * @param key   the attribute name
     * @param value the attribute value
     */
    public void setAttribute(String key, Object value) {
        if (value == null) {
            session.setAttribute(key, null);
            return;
        }

        Object toStore = value;
        if (ENCRYPT && !NO_ENCRYPT.contains(key)) {
            try {
                String jsonValue = JacksonUtil.getObjectMapper().writeValueAsString(value);
                toStore = Cryptography.encrypt(jsonValue);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize or encrypt attribute: " + key, e);
            }
        }
        session.setAttribute(key, toStore);
    }

    /**
     * Retrieves the attribute under {@code key}, decrypted if needed.
     *
     * @param key the attribute name
     * @return an Optional containing the stored value as a String, or empty
     */
    public Optional<String> getAttribute(String key) {
        Object raw = session.getAttribute(key);
        if (raw == null) {
            return Optional.empty();
        }

        if (ENCRYPT && raw instanceof String enc && !NO_ENCRYPT.contains(key)) {
            try {
                return Optional.of(Cryptography.decrypt(enc));
            } catch (Exception e) {
                throw new RuntimeException("Failed to decrypt attribute: " + key, e);
            }
        }
        return Optional.of(String.valueOf(raw));
    }

    /**
     * Retrieves the attribute under {@code key} as the specified type,
     * decrypted and deserialized if needed.
     *
     * @param <T>  the type of the attribute
     * @param key  the attribute name
     * @param type the Type from java.lang.reflect representing the type to
     *             deserialize to
     * @return an Optional containing the deserialized object, or empty
     */
    public <T> Optional<T> getAttribute(String key, Type type) {
        Object raw = session.getAttribute(key);
        if (raw == null) {
            return Optional.empty();
        }

        try {
            if (ENCRYPT && raw instanceof String enc && !NO_ENCRYPT.contains(key)) {
                String decrypted = Cryptography.decrypt(enc);
                JavaType javaType = JacksonUtil.getObjectMapper().constructType(type);
                return Optional.of(JacksonUtil.getObjectMapper().readValue(decrypted, javaType));
            }
            JavaType javaType = JacksonUtil.getObjectMapper().constructType(type);
            return Optional.of(JacksonUtil.getObjectMapper().convertValue(raw, javaType));
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize attribute: " + key, e);
        }
    }

    /**
     * Removes the attribute under {@code key}.
     *
     * @param key the attribute name
     */
    public void removeAttribute(String key) {
        session.removeAttribute(key);
    }

    /**
     * Securely clears all session attributes by setting them to null first,
     * then removing them completely. This helps ensure sensitive data
     * doesn't linger in memory.
     */
    public void clearAllAttributes() {
        try {
            var attributeNames = Collections.list(session.getAttributeNames());

            for (String attributeName : attributeNames) {
                try {
                    session.setAttribute(attributeName, null);
                } catch (Exception e) {
                    continue; // Continue cleanup even if individual attribute fails
                }
            }

            for (String attributeName : attributeNames) {
                try {
                    session.removeAttribute(attributeName);
                } catch (Exception e) {
                    continue; // Continue cleanup even if individual attribute fails
                }
            }
        } catch (IllegalStateException e) {
        }
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
     * Invalidates the wrapped session after clearing all attributes.
     */
    public void invalidate() {
        clearAllAttributes();
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