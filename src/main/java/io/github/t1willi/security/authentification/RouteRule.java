package io.github.t1willi.security.authentification;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import io.github.t1willi.context.JoltContext;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents a route-based authentication rule with failure handling
 * capabilities.
 * This class defines authentication requirements for specific URL patterns and
 * HTTP methods.
 */
@Getter
@ToString
public final class RouteRule {
    private final String pattern;
    private final boolean any;
    private final Set<String> methods;
    private final AuthStrategy strategy;
    private final boolean permitAll;
    private final boolean denyAll;
    private final Function<JoltContext, JoltContext> onFailureHandler;
    private final Map<String, Object> credentials;

    /**
     * Creates a new RouteRule with a functional failure handler.
     *
     * @param pattern          The URL pattern to match (supports wildcards like
     *                         "/api/**")
     * @param any              Whether this rule applies to any route
     * @param methods          Set of HTTP methods this rule applies to (null for
     *                         all methods)
     * @param strategy         The authentication strategy to use
     * @param permitAll        Whether to allow unrestricted access
     * @param denyAll          Whether to deny all access
     * @param onFailureHandler Function to handle authentication failures
     * @param credentials      Additional credentials required for authentication
     */
    public RouteRule(String pattern,
            boolean any,
            Set<String> methods,
            AuthStrategy strategy,
            boolean permitAll,
            boolean denyAll,
            Function<JoltContext, JoltContext> onFailureHandler,
            Map<String, Object> credentials) {
        this.pattern = pattern;
        this.any = any;
        this.methods = methods;
        this.strategy = strategy;
        this.permitAll = permitAll;
        this.denyAll = denyAll;
        this.onFailureHandler = onFailureHandler;
        this.credentials = credentials != null ? Map.copyOf(credentials) : Map.of();
    }

    /**
     * Creates a new RouteRule with a redirect-based failure handler.
     *
     * @param pattern     The URL pattern to match
     * @param any         Whether this rule applies to any route
     * @param methods     Set of HTTP methods this rule applies to
     * @param strategy    The authentication strategy to use
     * @param permitAll   Whether to allow unrestricted access
     * @param denyAll     Whether to deny all access
     * @param redirectTo  URL to redirect to on authentication failure (null for no
     *                    redirect)
     * @param credentials Additional credentials required for authentication
     */
    public RouteRule(String pattern, boolean any, Set<String> methods, AuthStrategy strategy,
            boolean permitAll, boolean denyAll, String redirectTo, Map<String, Object> credentials) {
        this(pattern, any, methods, strategy, permitAll, denyAll,
                redirectTo != null && !redirectTo.isEmpty() ? ctx -> {
                    ctx.redirect(redirectTo);
                    return ctx;
                } : null, credentials);
    }

    /**
     * Creates a new RouteRule with an OnAuthFailure handler.
     *
     * @param pattern        The URL pattern to match
     * @param any            Whether this rule applies to any route
     * @param methods        Set of HTTP methods this rule applies to
     * @param strategy       The authentication strategy to use
     * @param permitAll      Whether to allow unrestricted access
     * @param denyAll        Whether to deny all access
     * @param redirectTo     URL to redirect to on authentication failure (null for
     *                       no redirect)
     * @param credentials    Additional credentials required for authentication
     * @param failureHandler OnAuthFailure handler to execute on authentication
     *                       failure
     */
    public RouteRule(String pattern, boolean any, Set<String> methods, AuthStrategy strategy,
            boolean permitAll, boolean denyAll, String redirectTo, Map<String, Object> credentials,
            OnAuthFailure failureHandler) {
        this.pattern = pattern;
        this.any = any;
        this.methods = methods;
        this.strategy = strategy;
        this.permitAll = permitAll;
        this.denyAll = denyAll;
        this.credentials = credentials != null ? Map.copyOf(credentials) : Map.of();
        this.onFailureHandler = createCombinedFailureHandler(redirectTo, failureHandler);
    }

    /**
     * Creates a combined failure handler that handles both redirect and custom
     * OnAuthFailure logic.
     *
     * @param redirectTo     URL to redirect to (can be null)
     * @param failureHandler Custom failure handler (can be null)
     * @return Combined failure handler function
     */
    private Function<JoltContext, JoltContext> createCombinedFailureHandler(String redirectTo,
            OnAuthFailure failureHandler) {
        return ctx -> {
            if (failureHandler != null) {
                failureHandler.handle(ctx);
            }

            if (redirectTo != null && !redirectTo.isEmpty()) {
                ctx.redirect(redirectTo);
            }

            return ctx;
        };
    }

    /**
     * Handles authentication failure by executing the configured failure handler.
     *
     * @param ctx The JoltContext of the current request
     * @return true if a failure handler was executed, false otherwise
     */
    public boolean handleFailure(JoltContext ctx) {
        if (onFailureHandler != null) {
            onFailureHandler.apply(ctx);
            return true;
        }
        return false;
    }
}