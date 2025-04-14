package io.github.t1willi.security.session;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import io.github.t1willi.core.JoltDispatcherServlet;
import io.github.t1willi.database.Database;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.routing.context.JoltContext;
import jakarta.servlet.http.HttpSession;

public class Session {
    private static final Logger logger = Logger.getLogger(Session.class.getName());

    private static final SessionConfig config = JoltContainer.getInstance().getBean(SessionConfig.class);
    private static SessionStorage storage;

    static {
        initializeStorage();
    }

    public static void configure(SessionConfig newConfig) {
        Objects.requireNonNull(newConfig, "SessionConfig cannot be null.");
        Session.config // Self
                .withSessionTableName(newConfig.getSessionTableName())
                .withStorageType(newConfig.getStorageType())
                .withFileStoragePath(newConfig.getFileStoragePath())
                .withSessionLifetime(newConfig.getSessionLifetime());
        initializeStorage();
    }

    public static void set(String key, Object value) {
        String sessionId = ensureSession();
        getAttributes(sessionId).put(key, value);
    }

    public static void setAll(Map<String, Object> attributes) {
        String sessionId = ensureSession();
        getAttributes(sessionId).clear();
        getAttributes(sessionId).putAll(attributes);
    }

    public static void remove(String key) {
        String sessionId = ensureSession();
        getAttributes(sessionId).remove(key);
    }

    public static Object get(String key) {
        String sessionId = ensureSession();
        return getAttributes(sessionId).get(key);
    }

    public static Object getOrDefault(String key, Object defaultValue) {
        Object value = get(key);
        return value != null ? value : defaultValue;
    }

    public static void destroy() {
        JoltContext ctx = getContext();
        String sessionId = ctx.getCookieValue("JOLTSESSIONID").orElse(null);
        if (sessionId != null) {
            storage.deleteSession(sessionId);
            ctx.removeCookie("JOLTSESSIONID");
            HttpSession httpSession = ctx.getRequest().getSession(false);
            if (httpSession != null) {
                httpSession.invalidate();
            }
        }
    }

    public static boolean exists() {
        JoltContext ctx = getContext();
        String sessionId = ctx.getCookieValue("JOLTSESSIONID").orElse(null);
        if (sessionId == null) {
            return false;
        }
        HttpSession httpSession = ctx.getRequest().getSession(false);
        return httpSession != null && storage.loadSession(sessionId) != null;
    }

    public static boolean isAuthenticated() {
        Boolean isAuthenticated = (Boolean) get("isAuthenticated");
        return isAuthenticated != null && isAuthenticated;
    }

    static void syncSession() {
        JoltContext ctx = getContext();
        String sessionId = ctx.getCookieValue("JOLTSESSIONID").orElse(null);
        if (sessionId != null) {
            HttpSession httpSession = ctx.getRequest().getSession(false);
            if (httpSession != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> attributes = (Map<String, Object>) httpSession
                        .getAttribute("joltSessionAttributes");
                if (attributes != null) {
                    storage.saveSession(sessionId, attributes);
                }
            }
        }
    }

    private static Map<String, Object> getAttributes(String sessionId) {
        HttpSession httpSession = getContext().getRequest().getSession(false);
        @SuppressWarnings("unchecked")
        Map<String, Object> attributes = (Map<String, Object>) httpSession.getAttribute("attributes");
        if (attributes == null) {
            attributes = storage.loadSession(sessionId);
            if (attributes == null) {
                attributes = new HashMap<>();
                storage.saveSession(sessionId, attributes);
            }
            httpSession.setAttribute("joltSessionAttributes", attributes);
        }
        return attributes;
    }

    private static String ensureSession() {
        JoltContext ctx = getContext();
        HttpSession httpSession = ctx.getRequest().getSession(true);
        String sessionId = httpSession.getId();

        String existingSessionId = ctx.getCookieValue("JOLTSESSIONID").orElse(null);
        if (existingSessionId == null || storage.loadSession(existingSessionId) == null) {
            ctx.addCookie()
                    .setName("JOLTSESSIONID")
                    .setValue(sessionId)
                    .httpOnly(true)
                    .secure(true)
                    .path("/")
                    .sameSite("Strict")
                    .build();

            storage.saveSession(sessionId, new HashMap<>());
        } else {
            sessionId = existingSessionId;
        }

        return sessionId;
    }

    private static JoltContext getContext() {
        JoltContext ctx = JoltDispatcherServlet.getCurrentContext();
        if (ctx == null) {
            throw new IllegalStateException(
                    "No JoltContext available for the Session; ensure this is called within a request context");
        }
        return ctx;
    }

    private static void initializeStorage() {
        if (Database.getInstance().isInitialized()) {
            try {
                storage = new DatabaseSessionStorage(config);
                return;
            } catch (Exception e) {
                logger.warning(
                        () -> "Session storage initialization failed with database. Falling back to in-memory storage.");
            }
        }
        config.withStorageType(SessionConfig.StorageType.FILE);
        storage = new FileSessionStorage(config);
    }

}
