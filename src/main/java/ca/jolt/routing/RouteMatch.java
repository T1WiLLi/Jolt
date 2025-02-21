package ca.jolt.routing;

import java.util.regex.Matcher;

public final class RouteMatch {
    private final Route route;
    private final Matcher matcher;

    public RouteMatch(Route route, Matcher matcher) {
        this.route = route;
        this.matcher = matcher;
    }

    public Route route() {
        return route;
    }

    public Matcher matcher() {
        return matcher;
    }
}
