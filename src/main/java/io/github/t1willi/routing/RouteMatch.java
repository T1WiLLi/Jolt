package io.github.t1willi.routing;

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
 *
 * @see Route
 * @see RouteHandler
 * @since 1.0
 */
public final record RouteMatch(Route route, Matcher matcher) {
}
