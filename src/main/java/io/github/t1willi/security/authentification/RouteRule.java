package io.github.t1willi.security.authentification;

import java.util.Set;
import java.util.function.Function;

import io.github.t1willi.context.JoltContext;
import lombok.Getter;
import lombok.ToString;

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

    public RouteRule(String pattern,
            boolean any,
            Set<String> methods,
            AuthStrategy strategy,
            boolean permitAll,
            boolean denyAll,
            Function<JoltContext, JoltContext> onFailureHandler) {
        this.pattern = pattern;
        this.any = any;
        this.methods = methods;
        this.strategy = strategy;
        this.permitAll = permitAll;
        this.denyAll = denyAll;
        this.onFailureHandler = onFailureHandler;
    }
}
