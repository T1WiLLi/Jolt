package io.github.t1willi.context;

import java.util.function.Predicate;

/**
 * A specialized {@link JoltHttpContextValue} for query parameters.
 * Provides convenience methods to handle default values and type conversions
 * (e.g., int, double) safely.
 *
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * int page = ctx.query("page").asIntOrDefault(1);
 * }</pre>
 *
 * @see JoltContext
 * @see JoltHttpContextValue
 * @author William Beaudin
 * @since 1.0
 */
public final class QueryContextValue extends JoltHttpContextValue {
    public QueryContextValue(String value) {
        super(value);
    }

    public String orDefaultIf(Predicate<String> condition, String defaultValue) {
        return value.orElseGet(() -> condition.test(defaultValue) ? defaultValue : null);
    }

    public boolean asBooleanOrDefault(boolean defaultValue) {
        if (!isPresent()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.get().trim());
    }
}