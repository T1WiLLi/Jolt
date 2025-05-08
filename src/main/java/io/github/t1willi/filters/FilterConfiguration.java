package io.github.t1willi.filters;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.filters.security.AuthenticationFilter;
import io.github.t1willi.filters.security.CorsFilter;
import io.github.t1willi.filters.security.CsrfFilter;
import io.github.t1willi.filters.security.MaxRequestFilter;
import io.github.t1willi.filters.security.NonceFilter;
import io.github.t1willi.filters.security.SecureHeadersFilter;
import io.github.t1willi.utils.Constant;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

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
     * Offset for internal filters to ensure they run before user-defined filters.
     * Internal filters use orders from 1 to INTERNAL_FILTER_OFFSET.
     * User-defined filters start at INTERNAL_FILTER_OFFSET + 1.
     */
    private static final int INTERNAL_FILTER_OFFSET = Constant.Filter.INTERNAL_FILTER_OFFSET;

    /**
     * A list of routes to be excluded from filtering.
     */
    @Getter
    protected final List<String> excludedRoutes = new ArrayList<>();

    /**
     * A collection of predicates used to determine whether a route
     * should be excluded based on the {@link JoltContext}.
     */
    @Getter
    protected final List<Predicate<JoltContext>> exclusionPredicates = new ArrayList<>();

    /**
     * Maintains filter classes mapped to their assigned order.
     */
    protected final Map<Class<? extends JoltFilter>, Integer> filterOrders = new HashMap<>();

    /**
     * Performs any necessary filter configuration steps.
     * <p>
     * Intended to be overridden by subclasses providing
     * additional setup logic.
     */
    public abstract void configure();

    public FilterConfiguration() {
        filterOrders.put(MaxRequestFilter.class, 1); // Runs first: validate request limits
        filterOrders.put(CorsFilter.class, 2); // Runs second: handle CORS
        filterOrders.put(NonceFilter.class, 3); // Runs third: generate nonce for CSP
        filterOrders.put(CsrfFilter.class, 4); // Runs fourth: validate CSRF token
        filterOrders.put(AuthenticationFilter.class, 5); // Runs fifth: enforce route auth rules
        filterOrders.put(SecureHeadersFilter.class, 6); // Runs last: add security headers
    }

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
    public FilterConfiguration excludeIf(Predicate<JoltContext> routePredicate) {
        exclusionPredicates.add(routePredicate);
        return this;
    }

    /**
     * Assigns an order to the given filter class. Filters with lower
     * order values are processed earlier.
     * <p>
     * If the order is less than or equal to INTERNAL_FILTER_OFFSET, it is treated
     * as an internal filter order. Otherwise, it is adjusted to start after
     * INTERNAL_FILTER_OFFSET to ensure internal filters run first.
     *
     * @param order       The order to assign
     * @param filterClass The filter class to order
     * @return This configuration instance for method chaining
     */
    public FilterConfiguration setOrder(int order, Class<? extends JoltFilter> filterClass) {
        if (order <= INTERNAL_FILTER_OFFSET) {
            // Internal filter order, use as-is
            filterOrders.put(filterClass, order);
        } else {
            // User-defined filter, adjust order to start after internal filters
            filterOrders.put(filterClass, order + INTERNAL_FILTER_OFFSET);
        }
        return this;
    }

    /**
     * Retrieves the assigned order for the specified filter.
     *
     * @param filter The filter instance
     * @return The order value, or {@code Integer.MAX_VALUE} if no order is set
     */
    public int getOrder(JoltFilter filter) {
        return filterOrders.getOrDefault(filter.getClass(), Integer.MAX_VALUE);
    }

    /**
     * Determines if the specified route should be excluded from filtering.
     *
     * @param context The current {@link JoltContext}
     * @return {@code true} if the route is excluded, {@code false} otherwise
     */
    public boolean shouldExcludeRoute(JoltContext context) {
        String path = context.rawRequest().getPathInfo() != null
                ? context.rawRequest().getPathInfo()
                : context.rawRequest().getServletPath();
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