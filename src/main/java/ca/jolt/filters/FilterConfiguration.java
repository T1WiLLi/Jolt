package ca.jolt.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import ca.jolt.routing.context.JoltHttpContext;
import lombok.Getter;

public abstract class FilterConfiguration {

    @Getter
    protected final List<String> excludedRoutes = new ArrayList<>();
    @Getter
    protected final List<Predicate<JoltHttpContext>> exclusionPredicates = new ArrayList<>();
    protected final Map<JoltFilter, Integer> filterOrders = new HashMap<>();

    public abstract void configure();

    public FilterConfiguration exclude(String... routes) {
        Collections.addAll(excludedRoutes, routes);
        return this;
    }

    public FilterConfiguration excludeIf(Predicate<JoltHttpContext> routePredicate) {
        exclusionPredicates.add(routePredicate);
        return this;
    }

    public FilterConfiguration setOrder(int order, JoltFilter filter) {
        filterOrders.put(filter, order);
        return this;
    }

    public int getOrder(JoltFilter filter) {
        return filterOrders.getOrDefault(filter, Integer.MAX_VALUE);
    }

    public boolean shouldExcludeRoute(JoltHttpContext context) {
        String path = context.getRequest().getPathInfo() != null ? context.getRequest().getPathInfo()
                : context.getRequest().getServletPath();

        path = (path == null || path.isEmpty()) ? "/" : path;

        if (excludedRoutes.contains(path)) {
            return true;
        }

        return exclusionPredicates.stream()
                .anyMatch(predicate -> predicate.test(context));
    }

    public void reset() {
        excludedRoutes.clear();
        exclusionPredicates.clear();
        filterOrders.clear();
    }
}