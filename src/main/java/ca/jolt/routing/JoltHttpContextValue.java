package ca.jolt.routing;

import java.util.Optional;

public class JoltHttpContextValue {
    private final Optional<String> optional;

    public JoltHttpContextValue(String value) {
        this.optional = Optional.ofNullable(value);
    }

    public Optional<String> optional() {
        return optional;
    }

    public String orDefault(String defaultValue) {
        return optional.orElse(defaultValue);
    }

    public <X extends Throwable> String orThrow(X ex) throws X {
        if (!optional.isPresent()) {
            throw ex;
        }
        return optional.get();
    }

    public boolean isPresent() {
        return optional.isPresent();
    }

    public String get() {
        return orThrow(new IllegalArgumentException("The value is not present"));
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

    public long asLongOrDefault(long defaultValue) {
        if (!isPresent()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(get());
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
