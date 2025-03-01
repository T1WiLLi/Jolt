package ca.jolt.routing;

import java.util.regex.Matcher;

public final record RouteMatch(Route route, Matcher matcher) {

}
