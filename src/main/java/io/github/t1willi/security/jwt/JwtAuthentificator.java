package io.github.t1willi.security.jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.github.t1willi.core.JoltDispatcherServlet;
import io.github.t1willi.exceptions.SessionIpMismatchException;
import io.github.t1willi.exceptions.SessionUserAgentMismatchException;
import io.github.t1willi.routing.context.JoltContext;
import io.github.t1willi.server.config.ConfigurationManager;

public class JwtAuthentificator {

    private static final String COOKIE_NAME = ConfigurationManager.getInstance().getProperty("security.jwt.name",
            "JWT_TOKEN");

    private static final Duration EXPIRATION_TIME = Duration.ofMinutes(10);

    private static final String KEY_IP = "ipAddress";
    private static final String KEY_USER_AGENT = "userAgent";
    private static final String KEY_SUB = "sub";

    public static void set(String key, Object value) {
        JoltContext ctx = ctx();
        Map<String, Object> claims = getAll();
        verifyIntegrity(ctx, claims);
        claims.put(key, value);
        issuesToken(ctx, claims);
    }

    public static Object get(String key) {
        JoltContext ctx = ctx();
        Map<String, Object> claims = getAll();
        verifyIntegrity(ctx, claims);
        Object value = claims.get(key);
        return value != null ? value.toString() : null;
    }

    private static JoltContext ctx() {
        return JoltDispatcherServlet.getCurrentContext();
    }

    private static Map<String, Object> getAll() {
        JoltContext ctx = ctx();
        Optional<String> token = ctx.getCookieValue(COOKIE_NAME);
        if (token.isPresent()) {
            String jwt = token.get();
            if (JwtToken.verify(jwt)) {
                return new HashMap<>(JwtToken.getClaims(jwt));
            }
        }
        return new HashMap<>();
    }

    private static void issuesToken(JoltContext ctx, Map<String, Object> claims) {
        Object subVal = claims.get(KEY_SUB);
        if (subVal == null) {
            throw new IllegalArgumentException("Missing subject in claims");
        }
        String subject = subVal.toString();
        claims.put(KEY_IP, ctx.clientIp());
        claims.put(KEY_USER_AGENT, ctx.userAgent());
        claims.put("iat", Instant.now().getEpochSecond());
        claims.put("exp", Instant.now().plus(EXPIRATION_TIME).getEpochSecond());
        String token = JwtToken.jwe(subject, claims);
        ctx.addCookie()
                .setName(COOKIE_NAME)
                .setValue(token)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .maxAge((int) EXPIRATION_TIME.getSeconds())
                .path("/")
                .build();
    }

    private static void verifyIntegrity(JoltContext context, Map<String, Object> claims) {
        String storedIp = (String) claims.get(KEY_IP);
        String storedUa = (String) claims.get(KEY_USER_AGENT);
        if (storedIp != null && !context.clientIp().equals(storedIp)) {
            throw new SessionIpMismatchException();
        }
        if (storedUa != null && !context.userAgent().equals(storedUa)) {
            throw new SessionUserAgentMismatchException();
        }
    }

    private JwtAuthentificator() {
        // private constructor to prevent instantiation
    }
}
