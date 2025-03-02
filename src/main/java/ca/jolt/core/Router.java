package ca.jolt.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.logging.Logger;

import ca.jolt.exceptions.DuplicateRouteException;
import ca.jolt.injector.annotation.JoltBean;
import ca.jolt.routing.Route;
import ca.jolt.routing.RouteHandler;
import ca.jolt.routing.RouteMatch;

/**
 * The {@code Router} is responsible for managing and matching routes
 * against HTTP method/path patterns within a Jolt application. It
 * supports the standard HTTP verbs ({@code GET}, {@code POST},
 * {@code PUT}, {@code DELETE}) and allows registering both
 * {@link RouteHandler} instances and {@link Supplier} objects as
 * route handlers.
 * 
 * <p>
 * Routes are stored in an internal list, and this class provides
 * methods to determine which route should handle an incoming request
 * and which HTTP methods are allowed for a particular path.
 * Duplicate routes are prevented by {@link #addRoute(Route)}.
 * </p>
 * 
 * <p>
 * The router is exposed as a bean via the {@link JoltBean} annotation
 * so that it can be injected where needed, such as in {@code JoltApplication}
 * or {@code JoltDispatcherServlet}.
 * </p>
 * 
 * @author William Beaudin
 * @since 1.0
 */
@JoltBean
public final class Router {

    private static final Logger logger = Logger.getLogger(Router.class.getName());

    /**
     * Internal list storing all registered routes.
     */
    private final List<Route> routes = new ArrayList<>();

    /**
     * Constructs a new {@code Router} with an empty list of routes.
     * <p>
     * Primarily used by the dependency injection mechanism.
     */
    public Router() {
        // Empty constructor for DI.
    }

    /**
     * Registers a new HTTP {@code GET} route.
     *
     * @param path
     *                The path pattern (potentially including placeholders such as
     *                <code>{id:int}</code>).
     * @param handler
     *                A {@link RouteHandler} that processes the request and produces
     *                a response.
     * @return
     *         The current {@code Router} instance, for fluent chaining.
     * @throws DuplicateRouteException
     *                                 If another route with the same HTTP method
     *                                 and path is already registered.
     */
    public Router get(String path, RouteHandler handler) {
        addRoute(new Route("GET", path, handler));
        return this;
    }

    /**
     * Registers a new HTTP {@code GET} route where the response is produced
     * by a {@link Supplier} (useful for simple static responses).
     *
     * @param path
     *                 The path pattern to match.
     * @param supplier
     *                 A {@link Supplier} that returns a response object.
     * @return
     *         The current {@code Router} instance, for fluent chaining.
     * @throws DuplicateRouteException
     *                                 If another route with the same HTTP method
     *                                 and path is already registered.
     */
    public Router get(String path, Supplier<Object> supplier) {
        return get(path, RouteHandler.fromSupplier(supplier));
    }

    /**
     * Registers a new HTTP {@code POST} route.
     *
     * @param path
     *                The path pattern to match.
     * @param handler
     *                A {@link RouteHandler} that processes the request and produces
     *                a response.
     * @return
     *         The current {@code Router} instance, for fluent chaining.
     * @throws DuplicateRouteException
     *                                 If another route with the same HTTP method
     *                                 and path is already registered.
     */
    public Router post(String path, RouteHandler handler) {
        addRoute(new Route("POST", path, handler));
        return this;
    }

    /**
     * Registers a new HTTP {@code POST} route with a {@link Supplier} for the
     * response.
     *
     * @param path
     *                 The path pattern to match.
     * @param supplier
     *                 A {@link Supplier} that returns a response object.
     * @return
     *         The current {@code Router} instance, for fluent chaining.
     * @throws DuplicateRouteException
     *                                 If another route with the same HTTP method
     *                                 and path is already registered.
     */
    public Router post(String path, Supplier<Object> supplier) {
        return post(path, RouteHandler.fromSupplier(supplier));
    }

    /**
     * Registers a new HTTP {@code PUT} route.
     *
     * @param path
     *                The path pattern to match.
     * @param handler
     *                A {@link RouteHandler} that processes the request and produces
     *                a response.
     * @return
     *         The current {@code Router} instance, for fluent chaining.
     * @throws DuplicateRouteException
     *                                 If another route with the same HTTP method
     *                                 and path is already registered.
     */
    public Router put(String path, RouteHandler handler) {
        addRoute(new Route("PUT", path, handler));
        return this;
    }

    /**
     * Registers a new HTTP {@code PUT} route with a {@link Supplier} for the
     * response.
     *
     * @param path
     *                 The path pattern to match.
     * @param supplier
     *                 A {@link Supplier} that returns a response object.
     * @return
     *         The current {@code Router} instance, for fluent chaining.
     * @throws DuplicateRouteException
     *                                 If another route with the same HTTP method
     *                                 and path is already registered.
     */
    public Router put(String path, Supplier<Object> supplier) {
        return put(path, RouteHandler.fromSupplier(supplier));
    }

    /**
     * Registers a new HTTP {@code DELETE} route.
     *
     * @param path
     *                The path pattern to match.
     * @param handler
     *                A {@link RouteHandler} that processes the request and produces
     *                a response.
     * @return
     *         The current {@code Router} instance, for fluent chaining.
     * @throws DuplicateRouteException
     *                                 If another route with the same HTTP method
     *                                 and path is already registered.
     */
    public Router delete(String path, RouteHandler handler) {
        addRoute(new Route("DELETE", path, handler));
        return this;
    }

    /**
     * Registers a new HTTP {@code DELETE} route with a {@link Supplier} for the
     * response.
     *
     * @param path
     *                 The path pattern to match.
     * @param supplier
     *                 A {@link Supplier} that returns a response object.
     * @return
     *         The current {@code Router} instance, for fluent chaining.
     * @throws DuplicateRouteException
     *                                 If another route with the same HTTP method
     *                                 and path is already registered.
     */
    public Router delete(String path, Supplier<Object> supplier) {
        return delete(path, RouteHandler.fromSupplier(supplier));
    }

    /**
     * Returns a list of HTTP methods allowed for the specified path.
     * <p>
     * This is useful in scenarios such as responding to an
     * <em>HTTP 405 Method Not Allowed</em> by indicating which methods are
     * valid for the given path.
     *
     * @param path
     *             The request path to check.
     * @return
     *         A list of allowed HTTP methods; may be empty if no matching route is
     *         found.
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
     * Matches the incoming HTTP method and path against the registered routes.
     * <p>
     * Internally, it loops over all routes, checks if the methods match,
     * and then uses the route's {@link java.util.regex.Pattern} to see if
     * the path is a match. If both match, a {@link RouteMatch} is returned.
     *
     * @param method
     *                    The HTTP method, e.g., <code>"GET"</code> or
     *                    <code>"POST"</code>.
     * @param requestPath
     *                    The request path, e.g., <code>"/user/123"</code>.
     * @return
     *         A {@link RouteMatch} containing the matched route and matcher
     *         object, or {@code null} if none match.
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
     * Adds the specified {@link Route} to the internal list of routes.
     * <p>
     * Enforces uniqueness based on the route's HTTP method and path.
     *
     * @param route
     *              The {@link Route} to add.
     * @return
     *         The current {@code Router} instance, for fluent chaining.
     * @throws DuplicateRouteException
     *                                 If another route with the same HTTP method
     *                                 and path is already registered.
     */
    private Router addRoute(Route route) {
        for (Route r : routes) {
            if (r.getHttpMethod().equals(route.getHttpMethod()) && r.getPath().equals(route.getPath())) {
                throw new DuplicateRouteException(
                        "Route already exists for " + route.getHttpMethod() + " " + route.getPath());
            }
        }
        routes.add(route);
        logger.info(() -> "Registered route: " + route.getHttpMethod() + " " + route.getPath());
        return this;
    }
}