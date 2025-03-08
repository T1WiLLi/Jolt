package ca.jolt.routing.context;

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

    /**
     * Constructs a new {@link QueryContextValue}, wrapping the given string.
     *
     * @param value
     *              A query parameter value, potentially null.
     */
    public QueryContextValue(String value) {
        super(value);
    }

    /**
     * Returns the wrapped string value or a specified default if not present.
     *
     * @param defaultValue
     *                     The fallback value if this query parameter is absent.
     * @return
     *         The parameter value or the default value.
     */
    public String orDefault(String defaultValue) {
        return value.orElse(defaultValue);
    }

    /**
     * Returns the wrapped string value if present; otherwise checks a condition
     * against the default value. If the condition is satisfied, returns the
     * default value; otherwise returns null.
     *
     * @param condition
     *                     A predicate tested against the default value.
     * @param defaultValue
     *                     The fallback value if the parameter is not present.
     * @return
     *         The parameter value if present, or the default value if the parameter
     *         is missing and the predicate returns true.
     */
    public String orDefaultIf(Predicate<String> condition, String defaultValue) {
        return value.orElseGet(() -> condition.test(defaultValue) ? defaultValue : null);
    }

    /**
     * Parses the parameter as an integer, or returns the specified default if
     * the value is absent or not a valid integer.
     *
     * @param defaultValue
     *                     The fallback integer.
     * @return
     *         The parsed integer or the default.
     */
    public int asIntOrDefault(int defaultValue) {
        if (!isPresent()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(get());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Parses the parameter as a double, or returns the specified default if
     * the value is absent or not a valid double.
     *
     * @param defaultValue
     *                     The fallback double.
     * @return
     *         The parsed double or the default.
     */
    public double asDoubleOrDefault(double defaultValue) {
        if (!isPresent()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(get());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Parses the parameter as a boolean, or returns the specified default if
     * the value is absent.
     * <p>
     * The method uses {@link Boolean#parseBoolean(String)} which returns true if
     * the trimmed value is
     * equal to "true" (ignoring case), and false otherwise.
     * </p>
     *
     * @param defaultValue The fallback boolean.
     * @return The parsed boolean or the default.
     */
    public boolean asBooleanOrDefault(boolean defaultValue) {
        if (!isPresent()) {
            return defaultValue;
        }
        // Boolean.parseBoolean returns false if the value is null or not "true"
        return Boolean.parseBoolean(get().trim());
    }
}