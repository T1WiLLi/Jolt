package io.github.t1willi.routing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * A record holding information about a matched route, including:
 * <ul>
 * <li>The {@link Route} that was matched.</li>
 * <li>The {@link Matcher} object containing the pattern match results,
 * which can be used to extract path parameters.</li>
 * </ul>
 *
 * <p>
 * This object is typically created internally during request matching
 * and used by the Jolt framework to invoke the appropriate
 * {@link RouteHandler}.
 *
 * @param route
 *                The matched {@link Route} object.
 * @param matcher
 *                The {@link Matcher} used to match the route's regex pattern.
 * @param params  The map of path parameters extracted from the matcher.
 *
 * @see Route
 * @see RouteHandler
 * @since 1.0
 */
public final record RouteMatch(Route route, Matcher matcher, Map<String, String> params) {
    public RouteMatch(Route route, Matcher matcher) {
        this(route, matcher, extractParams(route, matcher));
    }

    private static Map<String, String> extractParams(Route route, Matcher matcher) {
        Map<String, String> params = new HashMap<>();
        List<String> paramNames = route.getParamNames();
        if (matcher != null && matcher.groupCount() > 0 && paramNames != null) {
            for (int i = 0; i < paramNames.size(); i++) {
                params.put(paramNames.get(i), matcher.group(i + 1));
            }
        }
        return params;
    }
}
