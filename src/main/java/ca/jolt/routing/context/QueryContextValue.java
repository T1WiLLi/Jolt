package ca.jolt.routing.context;

import java.util.function.Predicate;

public final class QueryContextValue extends JoltHttpContextValue {

    public QueryContextValue(String value) {
        super(value);
    }

    public String orDefault(String defaultValue) {
        return value.orElse(defaultValue);
    }

    public String orDefaultIf(Predicate<String> condition, String defaultValue) {
        return value.orElseGet(() -> condition.test(defaultValue) ? defaultValue : null);
    }

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
}
