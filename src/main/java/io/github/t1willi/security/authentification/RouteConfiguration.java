package io.github.t1willi.security.authentification;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.t1willi.http.HttpMethod;
import io.github.t1willi.security.config.SecurityConfiguration;
import lombok.Getter;

/**
 * DSL for defining route-based authentication rules.
 * Linked from SecurityConfiguration via withRoutes().
 */
public class RouteConfiguration {
    private final SecurityConfiguration parent;

    @Getter
    private final List<RouteRule> rules = new ArrayList<>();

    public RouteConfiguration(SecurityConfiguration parent) {
        this.parent = parent;
    }

    /**
     * Match specific path (supports "/path/**" wildcard).
     */
    public RouteBuilder route(String pattern) {
        return new RouteBuilder(this, pattern, false);
    }

    /**
     * Match any path.
     */
    public RouteBuilder anyRoute() {
        return new RouteBuilder(this, "**", true);
    }

    /**
     * Return control to parent security config.
     */
    public SecurityConfiguration and() {
        return parent;
    }

    public class RouteBuilder {
        private final RouteConfiguration parent;
        private final String pattern;
        private final boolean any;
        private Set<String> methods;
        private AuthStrategy strategy;
        private boolean permitAll;
        private boolean denyAll;

        RouteBuilder(RouteConfiguration parent, String pattern, boolean any) {
            this.parent = parent;
            this.pattern = pattern;
            this.any = any;
        }

        /**
         * Restrict by HTTP methods.
         */
        public RouteBuilder methods(HttpMethod... m) {
            this.methods = new HashSet<>();
            for (HttpMethod h : m)
                this.methods.add(h.name());
            return this;
        }

        /**
         * Allow unrestricted access.
         */
        public RouteConfiguration permitAll() {
            this.permitAll = true;
            addRule();
            return parent;
        }

        /**
         * Deny all access.
         */
        public RouteConfiguration denyAll() {
            this.denyAll = true;
            addRule();
            return parent;
        }

        /**
         * Require session-based authentication.
         */
        public RouteConfiguration authenticated() {
            return authenticated(new SessionAuthStrategy());
        }

        /**
         * Require custom authentication strategy.
         */
        public RouteConfiguration authenticated(AuthStrategy strat) {
            this.strategy = strat;
            addRule();
            return parent;
        }

        private void addRule() {
            rules.add(new RouteRule(pattern, any, methods, strategy, permitAll, denyAll));
        }
    }
}