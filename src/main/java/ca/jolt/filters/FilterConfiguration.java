package ca.jolt.filters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import ca.jolt.routing.context.JoltHttpContext;
import lombok.Getter;

/**
 * Defines an abstract configuration mechanism for filters within
 * the Jolt framework. Provides capabilities to exclude specific
 * routes from filtering, dynamically exclude routes based on
 * predicates, and assign custom ordering to filters.
 * <p>
 * Subclasses should implement {@link #configure()} to perform
 * actual configuration steps.
 *
 * @since 1.0
 */
public abstract class FilterConfiguration {

    /**
     * A list of routes to be excluded from filtering.
     */
    @Getter
    protected final List<String> excludedRoutes = new ArrayList<>();

    /**
     * A collection of predicates used to determine whether a route
     * should be excluded based on the {@link JoltHttpContext}.
     */
    @Getter
    protected final List<Predicate<JoltHttpContext>> exclusionPredicates = new ArrayList<>();

    /**
     * Maintains filter instances mapped to their assigned order.
     */
    protected final Map<JoltFilter, Integer> filterOrders = new HashMap<>();

    /**
     * Performs any necessary filter configuration steps.
     * <p>
     * Intended to be overridden by subclasses providing
     * additional setup logic.
     */
    public abstract void configure();

    /**
     * Excludes the specified routes from filtering.
     *
     * @param routes One or more route paths to be excluded
     * @return This configuration instance for method chaining
     */
    public FilterConfiguration exclude(String... routes) {
        Collections.addAll(excludedRoutes, routes);
        return this;
    }

    /**
     * Excludes routes if they satisfy the provided predicate.
     *
     * @param routePredicate A predicate that, if {@code true}, excludes the route
     * @return This configuration instance for method chaining
     */
    public FilterConfiguration excludeIf(Predicate<JoltHttpContext> routePredicate) {
        exclusionPredicates.add(routePredicate);
        return this;
    }

    /**
     * Assigns an order to the given filter. Filters with lower
     * order values are processed earlier.
     *
     * @param order  The order to assign
     * @param filter The filter instance to order
     * @return This configuration instance for method chaining
     */
    public FilterConfiguration setOrder(int order, JoltFilter filter) {
        filterOrders.put(filter, order);
        return this;
    }

    /**
     * Retrieves the assigned order for the specified filter.
     *
     * @param filter The filter instance
     * @return The order value, or {@code Integer.MAX_VALUE} if no order is set
     */
    public int getOrder(JoltFilter filter) {
        return filterOrders.getOrDefault(filter, Integer.MAX_VALUE);
    }

    /**
     * Determines if the specified route should be excluded from filtering.
     *
     * @param context The current {@link JoltHttpContext}
     * @return {@code true} if the route is excluded, {@code false} otherwise
     */
    public boolean shouldExcludeRoute(JoltHttpContext context) {
        String path = context.getRequest().getPathInfo() != null
                ? context.getRequest().getPathInfo()
                : context.getRequest().getServletPath();
        path = (path == null || path.isEmpty()) ? "/" : path;

        if (excludedRoutes.contains(path)) {
            return true;
        }
        return exclusionPredicates.stream().anyMatch(predicate -> predicate.test(context));
    }

    /**
     * Resets all configured routes, predicates, and filter orders.
     */
    public void reset() {
        excludedRoutes.clear();
        exclusionPredicates.clear();
        filterOrders.clear();
    }
}