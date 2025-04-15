package io.github.t1willi.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;

/**
 * Represents a single route in the Jolt framework, pairing an HTTP method
 * and a path pattern to a {@link RouteHandler}.
 * <p>
 * <strong>Example usage:</strong>
 * 
 * <pre>{@code
 * get("/users/{id}", ctx -> {
 *     int userId = ctx.path("id").asInt();
 *     // handle logic
 *     return "User " + userId;
 * });
 * }</pre>
 *
 * @see RouteHandler
 * @see RouteMatch
 * @see io.github.core.Router
 * @author William Beaudin
 * @since 1.0
 */
@Getter
public final class Route {

    /**
     * The HTTP method (e.g., GET, POST, PUT, DELETE) associated with this route.
     */
    private final String httpMethod;

    /**
     * The original path string provided, potentially including placeholders
     * like <code>{id}</code>.
     */
    private final String path;

    /**
     * A compiled regular expression matching the route path.
     * Parameter placeholders are replaced with appropriate capturing groups.
     */
    private final Pattern pattern;

    /**
     * A list of parameter names extracted from the path (e.g., ["id"] for
     * "/users/{id}").
     */
    private final List<String> paramNames;

    /**
     * The {@link RouteHandler} that will be invoked if this route matches
     * the incoming request.
     */
    private final RouteHandler handler;

    /**
     * Constructs a new {@code Route} by analyzing the provided path for any
     * parameter placeholders, compiling them into a regex, and storing the
     * resulting {@link Pattern} and parameter names.
     *
     * @param httpMethod
     *                   The HTTP method for this route (e.g., "GET").
     * @param path
     *                   The route path, possibly containing placeholders (e.g.,
     *                   "/user/{id}").
     * @param handler
     *                   The function to handle incoming requests matching this
     *                   route.
     */
    public Route(String httpMethod, String path, RouteHandler handler) {
        this.httpMethod = httpMethod;
        this.path = normalizePath(path);
        this.handler = handler;

        RoutePattern compiled = compile(path);
        this.pattern = compiled.pattern;
        this.paramNames = compiled.paramNames;
    }

    /**
     * A private record-like class to group the compiled regex {@link Pattern}
     * and the extracted parameter names.
     */
    private static final class RoutePattern {
        Pattern pattern;
        List<String> paramNames;

        RoutePattern(Pattern pattern, List<String> paramNames) {
            this.pattern = pattern;
            this.paramNames = paramNames;
        }
    }

    private RoutePattern compile(String path) {
        List<String> names = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\{([a-zA-Z_][\\w]*)\\}").matcher(path);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String paramName = matcher.group(1);
            names.add(paramName);
            matcher.appendReplacement(sb, "([^/]+)");
        }

        matcher.appendTail(sb);
        return new RoutePattern(Pattern.compile("^" + sb.toString() + "$"), names);
    }

    private String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        String normalized = path.replaceAll("//+", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
