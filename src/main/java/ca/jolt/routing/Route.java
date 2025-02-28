package ca.jolt.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
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
        Pattern pattern = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)(?::(int|double))?\\}");
        Matcher matcher = pattern.matcher(path);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            String paramType = matcher.group(2);
            names.add(paramName);
            String replacement;
            if ("int".equalsIgnoreCase(paramType)) {
                replacement = "(-?\\d+)";
            } else if ("double".equalsIgnoreCase(paramType)) {
                replacement = "(-?\\d+(?:\\.\\d+)?)";
            } else {
                replacement = "([^/]+)";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return new RoutePattern(Pattern.compile(new String("^" + sb.toString() + "$")), names);
    }
}