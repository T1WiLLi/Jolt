package ca.jolt.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import lombok.Getter;

@Getter
public final class Route {

    private final String httpMethod;
    private final String path;
    private final Pattern pattern;
    private final List<String> paramNames;
    private final RouteHandler handler;

    public Route(String httpMethod, String path, RouteHandler handler) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.handler = handler;
        RoutePattern compiled = compile(path);
        this.pattern = compiled.pattern;
        this.paramNames = compiled.paramNames;
    }

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
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\{([^}]+)\\}");
        java.util.regex.Matcher m = p.matcher(path);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String name = m.group(1);
            names.add(name);
            m.appendReplacement(sb, "([^/]+)");
        }
        m.appendTail(sb);
        String regex = "^" + sb.toString() + "$";
        return new RoutePattern(Pattern.compile(regex), names);
    }
}