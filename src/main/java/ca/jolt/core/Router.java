package ca.jolt.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.regex.Matcher;

import ca.jolt.routing.Route;
import ca.jolt.routing.RouteHandler;
import ca.jolt.routing.RouteMatch;

public final class Router {

    private final List<Route> routes = new ArrayList<>();

    public Router get(String path, RouteHandler handler) {
        addRoute(new Route("GET", path, handler));
        return this;
    }

    public Router get(String path, Supplier<Object> supplier) {
        return get(path, RouteHandler.fromSupplier(supplier));
    }

    public Router post(String path, RouteHandler handler) {
        addRoute(new Route("POST", path, handler));
        return this;
    }

    public Router post(String path, Supplier<Object> supplier) {
        return post(path, RouteHandler.fromSupplier(supplier));
    }

    public Router put(String path, RouteHandler handler) {
        addRoute(new Route("PUT", path, handler));
        return this;
    }

    public Router put(String path, Supplier<Object> supplier) {
        return put(path, RouteHandler.fromSupplier(supplier));
    }

    public Router delete(String path, RouteHandler handler) {
        addRoute(new Route("DELETE", path, handler));
        return this;
    }

    public Router delete(String path, Supplier<Object> supplier) {
        return delete(path, RouteHandler.fromSupplier(supplier));
    }

    public List<String> getAllowedMethods(String path) {
        List<String> allowed = new ArrayList<>();
        for (Route route : routes) {
            if (route.getPattern().matcher(path).matches()) {
                allowed.add(route.getHttpMethod());
            }
        }
        return allowed;
    }

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

    private Router addRoute(Route route) {
        routes.add(route);
        return this;
    }
}
