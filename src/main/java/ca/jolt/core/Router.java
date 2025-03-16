package ca.jolt.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.logging.Logger;

import ca.jolt.exceptions.DuplicateRouteException;
import ca.jolt.http.HttpMethod;
import ca.jolt.injector.annotation.JoltBean;
import ca.jolt.routing.LifecycleEntry;
import ca.jolt.routing.Route;
import ca.jolt.routing.RouteHandler;
import ca.jolt.routing.RouteMatch;
import ca.jolt.routing.context.JoltContext;
import lombok.Getter;

/**
 * Manages and matches routes against HTTP method/path patterns
 * within a Jolt application. Supports the standard HTTP verbs
 * ({@code GET}, {@code POST}, {@code PUT}, {@code DELETE}) and
 * allows registering both {@link RouteHandler} and {@link Supplier}
 * as route handlers. Also supports grouping routes for organization
 * via {@link #group(String, Runnable)}.
 * <p>
 * Routes are stored internally and checked against incoming
 * requests to determine the correct handler. Duplicate routes
 * (same method and path) throw a {@link DuplicateRouteException}.
 * This router is exposed as a bean via {@link JoltBean} so it can
 * be injected where needed (e.g., in {@code JoltApplication} or
 * {@code JoltDispatcherServlet}).
 *
 * @author William
 * @since 1.0
 */
@JoltBean()
public final class Router {

    /**
     * The logger for this router.
     */
    private static final Logger logger = Logger.getLogger(Router.class.getName());

    /**
     * Internal list storing all "before" lifecycle handlers.
     */
    @Getter
    private final List<LifecycleEntry> beforeHandlers = new ArrayList<>();

    /**
     * Internal list storing all "after" lifecycle handlers.
     */
    @Getter
    private final List<LifecycleEntry> afterHandlers = new ArrayList<>();

    /**
     * Internal list storing all registered routes.
     */
    private final List<Route> routes = new ArrayList<>();

    /**
     * Maintains prefixes for grouped routes.
     */
    private final Deque<String> prefixes = new ArrayDeque<>();

    /**
     * Registers a "before" handler that applies only to the specified routes.
     *
     * @param handler A handler to run before each matching route.
     *                Receives a {@link JoltContext} for the current request.
     * @param routes  One or more route paths (e.g. "/doc", "/api") where
     *                the handler applies
     */
    public void before(Consumer<JoltContext> handler, String... routes) {
        beforeHandlers.add(new LifecycleEntry(Arrays.asList(routes), handler));
    }

    /**
     * Registers an "after" handler that applies only to the specified routes.
     *
     * @param handler A handler to run after each matching route.
     *                Receives a {@link JoltContext} for the current request.
     * @param routes  One or more route paths (e.g. "/doc", "/api") where
     *                the handler applies
     */
    public void after(Consumer<JoltContext> handler, String... routes) {
        afterHandlers.add(new LifecycleEntry(Arrays.asList(routes), handler));
    }

    /**
     * Registers a new HTTP {@code GET} route.
     *
     * @param path    The path pattern, possibly with placeholders
     * @param handler A {@link RouteHandler} to process the request
     * @throws DuplicateRouteException If a route with the same method and path
     *                                 exists
     */
    public void get(String path, RouteHandler handler) {
        addRoute(new Route("GET", fullPath(path), handler));
    }

    /**
     * Registers a new HTTP {@code POST} route.
     *
     * @param path    The path pattern to match
     * @param handler A {@link RouteHandler} to process the request
     * @throws DuplicateRouteException If a route with the same method and path
     *                                 exists
     */
    public void post(String path, RouteHandler handler) {
        addRoute(new Route("POST", fullPath(path), handler));
    }

    /**
     * Registers a new HTTP {@code PUT} route.
     *
     * @param path    The path pattern to match
     * @param handler A {@link RouteHandler} to process the request
     * @throws DuplicateRouteException If a route with the same method and path
     *                                 exists
     */
    public void put(String path, RouteHandler handler) {
        addRoute(new Route("PUT", fullPath(path), handler));
    }

    /**
     * Registers a new HTTP {@code DELETE} route.
     *
     * @param path    The path pattern to match
     * @param handler A {@link RouteHandler} to process the request
     * @throws DuplicateRouteException If a route with the same method and path
     *                                 exists
     */
    public void delete(String path, RouteHandler handler) {
        addRoute(new Route("DELETE", fullPath(path), handler));
    }

    /**
     * Register a new HTTP route with the specified method and path.
     * 
     * @param method  The HTTP method (e.g. {@code GET}, {@code POST}, etc.)
     * @param path    The path pattern to match
     * @param handler A {@link RouteHandler} to process the request
     */
    public void route(HttpMethod method, String path, RouteHandler handler) {
        addRoute(new Route(method.toString(), fullPath(path), handler));
    }

    /**
     * Groups multiple routes under a common path prefix.
     * <p>
     * Example usage:
     * 
     * <pre>{@code
     * group("/api", () -> {
     *     get("/users", UserController::getAll);
     *     post("/users", UserController::create);
     *     group("/admin", () -> {
     *         get("/dashboard", AdminController::dashboard);
     *     });
     * });
     * }</pre>
     *
     * @param prefix The path prefix for the grouped routes
     * @param group  A {@link Runnable} containing route definitions
     */
    public void group(String prefix, Runnable group) {
        prefixes.push(normalizePath(getCurrentPrefix() + prefix));
        try {
            group.run();
        } finally {
            prefixes.pop();
        }
    }

    /**
     * Returns a list of HTTP methods allowed for the specified path.
     * <p>
     * Useful when responding with HTTP 405, indicating which methods
     * are valid for a certain path.
     *
     * @param path The request path to check
     * @return A list of valid HTTP methods, or an empty list if none match
     */
    public List<String> getAllowedMethods(String path) {
        List<String> allowed = new ArrayList<>();
        for (Route route : routes) {
            if (route.getPattern().matcher(path).matches()) {
                allowed.add(route.getHttpMethod());
            }
        }
        return allowed;
    }

    /**
     * Matches the HTTP method and path against registered routes
     * using a {@link java.util.regex.Pattern}. Returns a
     * {@link RouteMatch} if successful or {@code null} if no match is found.
     *
     * @param method      The HTTP method (e.g., {@code "GET"})
     * @param requestPath The request path (e.g., {@code "/user/123"})
     * @return A {@link RouteMatch} if a route is matched, otherwise {@code null}
     */
    public RouteMatch match(String method, String requestPath) {
        String upperMethod = method.toUpperCase(Locale.ROOT);
        for (Route route : routes) {
            if (route.getHttpMethod().equals(upperMethod)) {
                Matcher m = route.getPattern().matcher(requestPath);
                if (m.matches()) {
                    return new RouteMatch(route, m);
                }
            }
        }
        return null;
    }

    /**
     * Adds a route to the internal list, checking for duplicates by HTTP method
     * and path.
     *
     * @param route The route to add
     * @throws DuplicateRouteException If a route with the same method/path exists
     */
    private void addRoute(Route route) {
        for (Route r : routes) {
            if (r.getHttpMethod().equals(route.getHttpMethod()) && r.getPath().equals(route.getPath())) {
                throw new DuplicateRouteException(
                        "Route already exists for " + route.getHttpMethod() + " " + route.getPath());
            }
        }
        routes.add(route);
        logger.info(() -> "Registered route: " + route.getHttpMethod() + " " + route.getPath());
    }

    /**
     * Combines the current prefix with the given path and normalizes it
     * by reducing consecutive slashes.
     *
     * @param path A route path to combine with the current prefix
     * @return A normalized path string
     */
    private String fullPath(String path) {
        return normalizePath(getCurrentPrefix() + path);
    }

    /**
     * Retrieves the top prefix from the stack if available, or an empty
     * string if none is present.
     *
     * @return The current route prefix
     */
    private String getCurrentPrefix() {
        return prefixes.isEmpty() ? "" : prefixes.peek();
    }

    /**
     * Normalizes a path by converting consecutive slashes to a single slash.
     *
     * @param path The path to normalize
     * @return A cleaned-up path
     */
    private String normalizePath(String path) {
        return path.replaceAll("//+", "/");
    }
}