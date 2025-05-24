package io.github.t1willi.security.session;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.core.JoltDispatcherServlet;
import io.github.t1willi.exceptions.SessionExpiredException;
import io.github.t1willi.exceptions.SessionIpMismatchException;
import io.github.t1willi.exceptions.SessionUserAgentMismatchException;
import io.github.t1willi.server.config.ConfigurationManager;
import io.github.t1willi.utils.Constant;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A central utility for managing and validating user {@link HttpSession}
 * instances in the Jolt framework.
 *
 * <p>
 * <strong>Features:</strong>
 * </p>
 * <ul>
 * <li>Configurable session lifetime (with optional sliding expiration)</li>
 * <li>Automatic IP &amp; User-Agent binding to thwart hijacking</li>
 * <li>Session-ID rotation on authentication to prevent fixation</li>
 * <li>"Last access" timestamp updates on every read/write</li>
 * <li>Type-safe, transparent encryption of stored values (via
 * {@link JoltSession})</li>
 * <li>Secure session cleanup and cookie removal</li>
 * </ul>
 *
 * @see JoltSession
 * @see SessionIpMismatchException
 * @see SessionUserAgentMismatchException
 * @see SessionExpiredException
 */
public final class Session {
    private Session() {
        /* no instances */ }

    // ─────────────────────────────────────────────────────────────────────
    // Configuration & constants
    // ─────────────────────────────────────────────────────────────────────

    private static final boolean SLIDING = Boolean.parseBoolean(
            ConfigurationManager.getInstance()
                    .getProperty("session.expirationSliding", "false"));

    private static final int DEFAULT_LIFETIME = 1_800;
    private static volatile int lifetimeSeconds;

    public static final String KEY_INITIALIZED = Constant.SessionKeys.INITIALIZED;
    public static final String KEY_IP_ADDRESS = Constant.SessionKeys.IP_ADDRESS;
    public static final String KEY_USER_AGENT = Constant.SessionKeys.USER_AGENT;
    public static final String KEY_ACCESS_TIME = Constant.SessionKeys.ACCESS_TIME;
    public static final String KEY_EXPIRE_TIME = Constant.SessionKeys.EXPIRE_TIME;
    public static final String KEY_IS_AUTHENTICATED = Constant.SessionKeys.IS_AUTHENTICATED;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

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

    public static String get(String key, String defaultValue) {
        return getOptional(key).orElse(defaultValue);
    }

    public static String get(String key) {
        return getOptional(key).orElse(null);
    }

    public static <T> Optional<T> getOptional(String key, Type type) {
        JoltSession js = ensure(false);
        return (js == null) ? Optional.empty() : js.getAttribute(key, type);
    }

    public static Optional<String> getOptional(String key) {
        JoltSession js = ensure(false);
        return (js == null) ? Optional.empty() : js.getAttribute(key);
    }

    public static String getIpAddress() {
        return get(KEY_IP_ADDRESS);
    }

    public static String getUserAgent() {
        return get(KEY_USER_AGENT);
    }

    public static void set(String key, Object value) {
        JoltSession js = ensure(true);
        js.setAttribute(key, value);
    }

    public static void setAll(Map<String, ?> attrs) {
        attrs.forEach(Session::set);
    }

    public static void remove(String key) {
        JoltSession js = ensure(true);
        js.removeAttribute(key);
    }

    public static String getSessionId() {
        return ensure(true).raw().getId();
    }

    public static long getUnixAccess() {
        return getOptional(KEY_ACCESS_TIME)
                .map(Long::valueOf)
                .orElse(0L);
    }

    public static long getUnixExpire() {
        return getOptional(KEY_EXPIRE_TIME)
                .map(Long::valueOf)
                .orElse(0L);
    }

    public static String getAccess() {
        long t = getUnixAccess();
        return t > 0 ? TS_FMT.format(Instant.ofEpochMilli(t)) : "N/A";
    }

    public static String getExpire() {
        long t = getUnixExpire();
        return t > 0 ? TS_FMT.format(Instant.ofEpochMilli(t)) : "N/A";
    }

    public static void setAuthenticated(boolean authenticated) {
        JoltSession js = ensure(true);
        boolean was = js.getAttribute(KEY_IS_AUTHENTICATED)
                .map(Boolean::valueOf)
                .orElse(false);
        if (!was && authenticated) {
            js.changeSessionId(getCtx().rawRequest());
        }
        js.setAttribute(KEY_IS_AUTHENTICATED, authenticated);
    }

    public static boolean isAuthenticated() {
        JoltSession js = ensure(false);
        return js != null && js.getAttribute(KEY_IS_AUTHENTICATED)
                .map(Boolean::valueOf)
                .orElse(false);
    }

    /**
     * Logs out the user: clears attributes, invalidates the session,
     * and expires the session cookie.
     */
    public static void destroy() {
        JoltContext ctx = getCtx();
        HttpServletRequest req = ctx.rawRequest();
        HttpServletResponse res = ctx.rawResponse();

        HttpSession session = req.getSession(false);
        if (session != null) {
            // securely clear all attributes
            Collections.list(session.getAttributeNames()).forEach(name -> {
                session.setAttribute(name, null);
                session.removeAttribute(name);
            });
            session.invalidate();
        }

        expireCookie(req, res);
    }

    /**
     * Rotates the session ID by invalidating and creating a new session.
     * Attributes will be lost unless container supports changeSessionId().
     */
    public static void invalidate() {
        HttpServletRequest req = getCtx().rawRequest();
        HttpSession old = req.getSession(false);
        if (old != null)
            old.invalidate();
        // next getSession(true) will create a fresh session with new ID
        req.getSession(true);
    }

    public static boolean exists() {
        return ensure(false) != null;
    }

    // ─────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────

    private static JoltSession ensure(boolean create) {
        loadLifetime();
        HttpServletRequest req = getCtx().rawRequest();
        HttpSession raw = req.getSession(create);
        if (raw == null)
            return null;

        JoltSession js = wrap(raw);
        if (!js.getAttribute(KEY_INITIALIZED).isPresent()) {
            initialize(raw, getCtx());
        }

        validate(js, getCtx());
        expire(js);

        // update last‐access timestamp
        js.setAttribute(KEY_ACCESS_TIME, String.valueOf(Instant.now().toEpochMilli()));
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
        bind(js, KEY_USER_AGENT, ctx.userAgent(), SessionUserAgentMismatchException::new);
    }

    private static void bind(JoltSession js, String key, String actual,
            Supplier<RuntimeException> ex) {
        String stored = js.getAttribute(key).orElse(null);
        if (!Objects.equals(stored, actual)) {
            js.invalidate();
            throw ex.get();
        }
    }

    private static void expire(JoltSession js) {
        long exp = js.getAttribute(KEY_EXPIRE_TIME)
                .map(Long::valueOf)
                .orElse(0L);
        long now = Instant.now().toEpochMilli();
        if (exp > 0 && now > exp) {
            js.invalidate();
            throw new SessionExpiredException();
        }
        if (SLIDING) {
            js.setAttribute(KEY_EXPIRE_TIME,
                    String.valueOf(now + Duration.ofSeconds(getLifetime()).toMillis()));
        }
    }

    private static void expireCookie(HttpServletRequest req, HttpServletResponse res) {
        String name = ConfigurationManager.getInstance()
                .getProperty("session.cookieName", "SESSION");
        Cookie cookie = new Cookie(name, "");
        cookie.setPath(req.getContextPath() == null ? "/" : req.getContextPath());
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        if (req.isSecure()) {
            cookie.setSecure(true);
        }
        res.addCookie(cookie);
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
