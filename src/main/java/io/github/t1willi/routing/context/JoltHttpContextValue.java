package io.github.t1willi.routing.context;

import java.util.Optional;

/**
 * An abstract base class representing a context-dependent value, such as
 * a path parameter or query parameter. It uses an {@link Optional} to
 * safely handle missing or null values.
 *
 * <p>
 * Subclasses typically provide additional convenience methods for
 * converting the value to other types (e.g., int, double) or providing
 * default values.
 *
 * @see PathContextValue
 * @see QueryContextValue
 */
public abstract class JoltHttpContextValue {

    /**
     * An optional string value, often representing a path or query parameter.
     */
    protected final Optional<String> value;

    /**
     * Constructs a new context value, wrapping the provided string
     * in an {@link Optional}.
     *
     * @param value
     *              The string value, possibly {@code null}.
     */
    protected JoltHttpContextValue(String value) {
        this.value = Optional.ofNullable(value);
    }

    /**
     * Indicates whether this value is present.
     *
     * @return
     *         {@code true} if the value is not null; {@code false} otherwise.
     */
    public boolean isPresent() {
        return value.isPresent();
    }

    @Override
    public String toString() {
        return value.orElse("");
    }
}