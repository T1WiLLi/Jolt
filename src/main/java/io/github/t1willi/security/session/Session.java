package io.github.t1willi.security.session;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.core.JoltDispatcherServlet;
import io.github.t1willi.exceptions.SessionExpiredException;
import io.github.t1willi.exceptions.SessionIpMismatchException;
import io.github.t1willi.exceptions.SessionUserAgentMismatchException;
import io.github.t1willi.server.config.ConfigurationManager;
import io.github.t1willi.utils.Constant;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A central utility for managing and validating user {@link HttpSession}
 * instances
 * in the Jolt framework.
 *
 * <p>
 * <strong>Features:</strong>
 * </p>
 * <ul>
 * <li>Configurable session lifetime (with optional sliding expiration)</li>
 * <li>Automatic IP &amp; User‑Agent binding to thwart hijacking</li>
 * <li>Session‑ID rotation on authentication to prevent fixation</li>
 * <li>“Last access” timestamp updates on every read/write</li>
 * <li>Type‑safe, transparent encryption of stored values (via
 * {@link JoltSession})</li>
 * </ul>
 *
 * @see JoltSession
 * @see SessionIpMismatchException
 * @see SessionUserAgentMismatchException
 * @see SessionExpiredException
 */
public final class Session {
    /**
     * When true, each access pushes expiration forward by {@link #getLifetime()}.
     */
    private static final boolean SLIDING = Boolean.parseBoolean(
            ConfigurationManager.getInstance()
                    .getProperty("session.expirationSliding", "false"));

    /** Default lifetime in seconds if mis‑configured or missing. */
    private static final int DEFAULT_LIFETIME = 1_800;

    /** Single‑run loader for {@code session.lifetime}. */
    private static volatile int lifetimeSeconds;

    /** Initialization flag in the session to avoid re‑init. */
    public static final String KEY_INITIALIZED = Constant.SessionKeys.INITIALIZED;
    public static final String KEY_IP_ADDRESS = Constant.SessionKeys.IP_ADDRESS;
    public static final String KEY_USER_AGENT = Constant.SessionKeys.USER_AGENT;
    public static final String KEY_ACCESS_TIME = Constant.SessionKeys.ACCESS_TIME;
    public static final String KEY_EXPIRE_TIME = Constant.SessionKeys.EXPIRE_TIME;
    public static final String KEY_IS_AUTHENTICATED = Constant.SessionKeys.IS_AUTHENTICATED;

    /** Formatter for human‑readable timestamps. */
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    static {
        loadLifetime();
    }

    private Session() {
        /* no instances */
    }

    /** Lazily loads {@code session.lifetime} exactly once. */
    private static void loadLifetime() {
        if (lifetimeSeconds == 0) {
            synchronized (Session.class) {
                if (lifetimeSeconds == 0) {
                    String raw = ConfigurationManager.getInstance()
                            .getProperty("session.lifetime");
                    try {
                        lifetimeSeconds = raw != null
                                ? Integer.parseInt(raw)
                                : DEFAULT_LIFETIME;
                        if (lifetimeSeconds <= 0)
                            throw new IllegalArgumentException();
                    } catch (Exception e) {
                        lifetimeSeconds = DEFAULT_LIFETIME;
                    }
                }
            }
        }
    }

    private static int getLifetime() {
        loadLifetime();
        return lifetimeSeconds;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Retrieves the value of the given session attribute, or returns
     * {@code defaultValue}
     * if missing or not a String.
     *
     * @param key          the session attribute name
     * @param defaultValue the fallback to return if attribute absent
     * @return the stored String or {@code defaultValue}
     */
    public static String get(String key, String defaultValue) {
        return getOptional(key).orElse(defaultValue);
    }

    /**
     * Retrieves the value of the given session attribute, or {@code null}
     * if missing.
     *
     * @param key the session attribute name
     * @return the stored String or {@code null}
     */
    public static String get(String key) {
        return getOptional(key).orElse(null);
    }

    /**
     * Retrieves the value of the given session attribute as an {@link Optional}.
     *
     * @param key the session attribute name
     * @return an Optional containing the stored String, or empty
     */
    public static Optional<String> getOptional(String key) {
        JoltSession js = ensure(false);
        return (js == null)
                ? Optional.empty()
                : js.getAttribute(key);
    }

    /**
     * Convenience for {@link #get(String)} on {@code ip_address}.
     * 
     * @return stored client IP or {@code null}
     */
    public static String getIpAddress() {
        return get(KEY_IP_ADDRESS);
    }

    /**
     * Convenience for {@link #get(String)} on {@code user_agent}.
     * 
     * @return stored UA or {@code null}
     */
    public static String getUserAgent() {
        return get(KEY_USER_AGENT);
    }

    /**
     * Stores a session attribute (and triggers initialization on first write).
     *
     * @param key   the attribute name
     * @param value the value to store (Strings may be encrypted)
     */
    public static void set(String key, Object value) {
        JoltSession js = ensure(true);
        js.setAttribute(key, value);
    }

    /**
     * Stores all entries of the provided map into the session.
     *
     * @param attrs map of attribute‑value pairs
     */
    public static void setAll(Map<String, ?> attrs) {
        attrs.forEach(Session::set);
    }

    /**
     * Removes the given attribute from the session.
     *
     * @param key the attribute name to remove
     */
    public static void remove(String key) {
        JoltSession js = ensure(true);
        js.removeAttribute(key);
    }

    /**
     * Returns the active session’s ID, optionally creating one if none exists.
     *
     * @return the HTTP session ID
     */
    public static String getSessionId() {
        return ensure(true).raw().getId();
    }

    /**
     * Returns the stored access timestamp as epoch millis, or 0 if missing.
     *
     * @return last‑access in ms
     */
    public static long getUnixAccess() {
        return getOptional(KEY_ACCESS_TIME)
                .map(Long::valueOf)
                .orElse(0L);
    }

    /**
     * Returns the stored expiration timestamp as epoch millis, or 0 if missing.
     *
     * @return expire time in ms
     */
    public static long getUnixExpire() {
        return getOptional(KEY_EXPIRE_TIME)
                .map(Long::valueOf)
                .orElse(0L);
    }

    /**
     * Returns a human‑readable “last access” time or “N/A”.
     *
     * @return formatted access timestamp
     */
    public static String getAccess() {
        long t = getUnixAccess();
        return t > 0
                ? TS_FMT.format(Instant.ofEpochMilli(t))
                : "N/A";
    }

    /**
     * Returns a human‑readable “expire” time or “N/A”.
     *
     * @return formatted expire timestamp
     */
    public static String getExpire() {
        long t = getUnixExpire();
        return t > 0
                ? TS_FMT.format(Instant.ofEpochMilli(t))
                : "N/A";
    }

    /**
     * Marks the session authenticated or not. If turning <em>on</em> (false→true),
     * rotates the session‑ID to prevent fixation.
     *
     * @param authenticated new authentication state
     */
    public static void setAuthenticated(boolean authenticated) {
        JoltSession js = ensure(true);
        boolean was = js.getAttribute(KEY_IS_AUTHENTICATED)
                .map(Boolean::valueOf)
                .orElse(false);
        if (!was && authenticated) {
            js.changeSessionId(getCtx().getRequest());
        }
        js.setAttribute(KEY_IS_AUTHENTICATED, authenticated);
    }

    /**
     * Checks whether the session is marked authenticated.
     *
     * @return true if authenticated
     */
    public static boolean isAuthenticated() {
        JoltSession js = ensure(false);
        return js != null && js.getAttribute(KEY_IS_AUTHENTICATED)
                .map(Boolean::valueOf)
                .orElse(false);
    }

    /**
     * Invalidates the current session (logout).
     */
    public static void destroy() {
        HttpSession s = getCtx().getRequest().getSession(false);
        if (s != null)
            s.invalidate();
    }

    /**
     * Fully invalidates and recreates a session, then re‑initializes it
     * and rotates its ID.
     */
    public static void invalidate() {
        JoltContext ctx = getCtx();
        HttpServletRequest req = ctx.getRequest();
        HttpSession old = req.getSession(false);
        if (old != null)
            old.invalidate();
        HttpSession neu = req.getSession(true);
        initialize(neu, ctx);
        wrap(neu).changeSessionId(req);
    }

    /**
     * Returns true if a session already exists for this request.
     *
     * @return true when unexpired session is present
     */
    public static boolean exists() {
        return ensure(false) != null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Internal pipeline — called by every public op
    // ─────────────────────────────────────────────────────────────────────

    private static JoltSession ensure(boolean create) {
        loadLifetime();
        JoltContext ctx = getCtx();
        HttpSession raw = ctx.getRequest().getSession(create);
        if (raw == null)
            return null;

        JoltSession js = wrap(raw);

        if (!js.getAttribute(KEY_INITIALIZED).isPresent()) {
            initialize(raw, ctx);
        }

        validate(js, ctx);
        expire(js);

        js.setAttribute(KEY_ACCESS_TIME, Instant.now().toEpochMilli());
        return js;
    }

    private static void initialize(HttpSession s, JoltContext ctx) {
        long now = Instant.now().toEpochMilli();
        s.setAttribute(KEY_INITIALIZED, true);
        s.setAttribute(KEY_IP_ADDRESS, ctx.clientIp());
        s.setAttribute(KEY_USER_AGENT, ctx.userAgent());
        s.setAttribute(KEY_ACCESS_TIME, now);
        s.setAttribute(KEY_EXPIRE_TIME, now + Duration.ofSeconds(getLifetime()).toMillis());
        s.setAttribute(KEY_IS_AUTHENTICATED, false);
    }

    private static void validate(JoltSession js, JoltContext ctx) {
        bind(js, KEY_IP_ADDRESS, ctx.clientIp(), SessionIpMismatchException::new);
        bind(js, KEY_USER_AGENT, ctx.userAgent(), SessionUserAgentMismatchException::new);
    }

    private static void bind(JoltSession js,
            String key,
            String actual,
            Supplier<RuntimeException> exceptionSupplier) {
        String stored = js.getAttribute(key).orElse(null);
        if (!Objects.equals(stored, actual)) {
            js.raw().invalidate();
            throw exceptionSupplier.get();
        }
    }

    private static void expire(JoltSession js) {
        Optional<String> raw = js.getAttribute(KEY_EXPIRE_TIME);
        long exp = raw.map(Long::parseLong).orElse(0L);
        long now = Instant.now().toEpochMilli();

        if (now > exp) {
            js.raw().invalidate();
            throw new SessionExpiredException();
        }

        if (SLIDING) {
            js.setAttribute(KEY_EXPIRE_TIME,
                    now + Duration.ofSeconds(getLifetime()).toMillis());
        }
    }

    private static JoltContext getCtx() {
        JoltContext ctx = JoltDispatcherServlet.getCurrentContext();
        if (ctx == null)
            throw new IllegalStateException("No JoltContext");
        return ctx;
    }

    private static JoltSession wrap(HttpSession s) {
        return new JoltSession(s);
    }
}
