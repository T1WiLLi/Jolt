package io.github.t1willi.security.session;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import io.github.t1willi.core.JoltDispatcherServlet;
import io.github.t1willi.exceptions.SessionExpiredException;
import io.github.t1willi.exceptions.SessionIpMismatchException;
import io.github.t1willi.exceptions.SessionUserAgentMismatchException;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.routing.context.JoltContext;
import io.github.t1willi.server.config.ConfigurationManager;
import jakarta.servlet.http.HttpSession;

public class Session {
    private static final Logger logger = Logger.getLogger(Session.class.getName());

    /** Key for marking that session has been initialized. */
    private static final String SESSION_INITIALIZED_KEY = "jolt_session_initialized";

    // Public constants for default session attribute keys
    public static final String IP_ADDRESS_KEY = "ip_address";
    public static final String USER_AGENT_KEY = "user_agent";
    public static final String ACCESS_TIME_KEY = "access_time";
    public static final String EXPIRE_TIME_KEY = "expire_time";
    /**
     * Key for storing the authentication status (Boolean) in the session
     * attributes.
     */
    public static final String IS_AUTHENTICATED_KEY = "is_authenticated";

    private static final int DEFAULT_SESSION_LIFETIME = 1800;
    private static volatile Integer sessionLifetime = null;
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void set(String key, Object value) {
        HttpSession session = ensureSession(true);
        session.setAttribute(key, value);
        session.setAttribute("_forceCommit", System.currentTimeMillis());
    }

    public static void setAll(Map<String, Object> attributes) {
        attributes.forEach(Session::set);
    }

    public static void remove(String key) {
        HttpSession session = ensureSession(true);
        session.removeAttribute(key);
    }

    public static Object get(String key) {
        HttpSession session = ensureSession(false);
        return session != null ? session.getAttribute(key) : null;
    }

    public static Object getOrDefault(String key, Object defaultValue) {
        try {
            Object value = get(key);
            return value != null ? value : defaultValue;
        } catch (SecurityException e) {
            return defaultValue;
        }
    }

    public static String getSessionId() {
        HttpSession session = ensureSession(true);
        return session.getId();
    }

    public static String getIpAddress() {
        return (String) get(IP_ADDRESS_KEY);
    }

    public static String getUserAgent() {
        return (String) get(USER_AGENT_KEY);
    }

    public static long getUnixAccess() {
        String ts = (String) get(ACCESS_TIME_KEY);
        return ts != null ? Long.parseLong(ts) : 0L;
    }

    public static long getUnixExpire() {
        String ts = (String) get(EXPIRE_TIME_KEY);
        return ts != null ? Long.parseLong(ts) : 0L;
    }

    public static String getAccess() {
        long ua = getUnixAccess();
        return ua != 0 ? TIMESTAMP_FORMAT.format(new Date(ua)) : "N/A";
    }

    public static String getExpire() {
        long ue = getUnixExpire();
        return ue != 0 ? TIMESTAMP_FORMAT.format(new Date(ue)) : "N/A";
    }

    public static void setAuthenticated(boolean authenticated) {
        set(IS_AUTHENTICATED_KEY, authenticated);
    }

    public static boolean isAuthenticated() {
        try {
            HttpSession session = ensureSession(false);
            if (session == null) {
                return false;
            }
            Boolean auth = (Boolean) get(IS_AUTHENTICATED_KEY);
            return Boolean.TRUE.equals(auth);
        } catch (SecurityException e) {
            return false;
        }
    }

    public static void destroy() {
        JoltContext ctx = getContext();
        HttpSession httpSession = ctx.getRequest().getSession(false);
        if (httpSession != null) {
            httpSession.invalidate();
        }
    }

    public static void invalidate() {
        JoltContext ctx = getContext();
        HttpSession oldSession = ctx.getRequest().getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession newSession = ctx.getRequest().getSession(true);
        initializeSession(newSession, ctx);
    }

    public static boolean exists() {
        return ensureSession(false) != null;
    }

    private static void initializeSessionLifetime() {
        if (sessionLifetime == null) {
            synchronized (Session.class) {
                if (sessionLifetime == null) {
                    String lifetimeStr = ConfigurationManager.getInstance().getProperty("session.lifetime");
                    try {
                        sessionLifetime = lifetimeStr != null ? Integer.parseInt(lifetimeStr)
                                : DEFAULT_SESSION_LIFETIME;
                        if (sessionLifetime <= 0) {
                            throw new IllegalArgumentException("Session lifetime must be positive");
                        }
                    } catch (NumberFormatException e) {
                        sessionLifetime = DEFAULT_SESSION_LIFETIME;
                    }
                }
            }
        }
    }

    private static void initializeSession(HttpSession session, JoltContext ctx) {
        long accessTime = System.currentTimeMillis();
        long expireTime = accessTime + (sessionLifetime * 1000L); // sessionLifetime is in seconds, convert to
                                                                  // milliseconds

        session.setAttribute(SESSION_INITIALIZED_KEY, true);
        session.setAttribute(IP_ADDRESS_KEY, ctx.clientIp());
        session.setAttribute(USER_AGENT_KEY, ctx.userAgent());
        session.setAttribute(ACCESS_TIME_KEY, String.valueOf(accessTime));
        session.setAttribute(EXPIRE_TIME_KEY, String.valueOf(expireTime));
        session.setAttribute(IS_AUTHENTICATED_KEY, false);
    }

    private static HttpSession ensureSession(boolean create) {
        initializeSessionLifetime();
        JoltContext ctx = getContext();
        HttpSession httpSession = ctx.getRequest().getSession(create);
        if (httpSession == null) {
            return null;
        }

        if (httpSession.getAttribute(SESSION_INITIALIZED_KEY) == null) {
            initializeSession(httpSession, ctx);
        }

        String storedIp = (String) httpSession.getAttribute(IP_ADDRESS_KEY);
        String currentIp = ctx.clientIp();
        if (!Objects.equals(currentIp, storedIp)) {
            logger.warning(() -> "IP address mismatch for session " + httpSession.getId() + ": expected "
                    + storedIp + ", got " + currentIp + ". Warning! This might indicate a session hijacking.");
            httpSession.invalidate();
            ctx.status(HttpStatus.FORBIDDEN);
            throw new SessionIpMismatchException();
        }

        String storedUserAgent = (String) httpSession.getAttribute(USER_AGENT_KEY);
        String currentUserAgent = ctx.userAgent();
        if (!Objects.equals(currentUserAgent, storedUserAgent)) {
            logger.warning(() -> "User-Agent mismatch for session " + httpSession.getId() + ": expected "
                    + storedUserAgent + ", got " + currentUserAgent
                    + ". Warning! This might indicate a session hijacking.");
            httpSession.invalidate();
            ctx.status(HttpStatus.FORBIDDEN);
            throw new SessionUserAgentMismatchException();
        }

        long expireTime = Long.parseLong((String) httpSession.getAttribute(EXPIRE_TIME_KEY));
        long currentTime = System.currentTimeMillis();
        if (currentTime > expireTime) {
            logger.warning("Session expired: " + httpSession.getId());
            httpSession.invalidate();
            ctx.status(HttpStatus.FORBIDDEN);
            throw new SessionExpiredException();
        }
        return httpSession;
    }

    private static JoltContext getContext() {
        JoltContext ctx = JoltDispatcherServlet.getCurrentContext();
        if (ctx == null) {
            throw new IllegalStateException(
                    "No JoltContext available for the Session; ensure this is called within a request context");
        }
        return ctx;
    }

    private Session() {
        // Private constructor to prevent instantiation
    }
}