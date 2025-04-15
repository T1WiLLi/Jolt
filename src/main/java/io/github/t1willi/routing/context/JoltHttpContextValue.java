package io.github.t1willi.routing.context;

import java.util.Optional;

/**
 * Represents an abstract context value in the Jolt HTTP routing framework.
 * This class encapsulates an optional string value and provides utility methods
 * to interact with the value in various formats, such as integers and doubles.
 * Subclasses can extend this class to define specific types of context values.
 */
public abstract class JoltHttpContextValue {

    /**
     * The optional string value encapsulated by this context value.
     */
    protected final Optional<String> value;

    /**
     * Constructs a new {@code JoltHttpContextValue} with the given string value.
     * If the provided value is {@code null}, the internal {@code Optional} will be
     * empty.
     *
     * @param value the string value to encapsulate, or {@code null}.
     */
    protected JoltHttpContextValue(String value) {
        this.value = Optional.ofNullable(value);
    }

    /**
     * Checks if the encapsulated value is present.
     *
     * @return {@code true} if the value is present, {@code false} otherwise.
     */
    public boolean isPresent() {
        return value.isPresent();
    }

    /**
     * Retrieves the encapsulated value.
     *
     * @return the encapsulated value, or {@code null} if the value is not present.
     */
    public String get() {
        return value.orElse(null);
    }

    /**
     * Retrieves the encapsulated value, or returns the specified default value
     * if the encapsulated value is not present.
     *
     * @param defaultValue the default value to return if the encapsulated value is
     *                     not present.
     * @return the encapsulated value, or the specified default value.
     */
    public String orDefault(String defaultValue) {
        return value.orElse(defaultValue);
    }

    /**
     * Attempts to parse the encapsulated value as an integer.
     * If the value is not present or cannot be parsed, returns {@code 0}.
     *
     * @return the parsed integer value, or {@code 0} if parsing fails or the value
     *         is not present.
     */
    public int asInt() {
        return asIntOrDefault(0);
    }

    /**
     * Attempts to parse the encapsulated value as an integer.
     * If the value is not present or cannot be parsed, returns the specified
     * default value.
     *
     * @param defaultValue the default value to return if parsing fails or the value
     *                     is not present.
     * @return the parsed integer value, or the specified default value.
     */
    public int asIntOrDefault(int defaultValue) {
        try {
            return value.map(Integer::parseInt).orElse(defaultValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Attempts to parse the encapsulated value as a double.
     * If the value is not present or cannot be parsed, returns {@code 0.0}.
     *
     * @return the parsed double value, or {@code 0.0} if parsing fails or the value
     *         is not present.
     */
    public double asDouble() {
        return asDoubleOrDefault(0.0);
    }

    /**
     * Attempts to parse the encapsulated value as a double.
     * If the value is not present or cannot be parsed, returns the specified
     * default value.
     *
     * @param defaultValue the default value to return if parsing fails or the value
     *                     is not present.
     * @return the parsed double value, or the specified default value.
     */
    public double asDoubleOrDefault(double defaultValue) {
        try {
            return value.map(Double::parseDouble).orElse(defaultValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Returns a string representation of the encapsulated value.
     * If the value is not present, returns an empty string.
     *
     * @return the string representation of the encapsulated value, or an empty
     *         string if not present.
     */
    @Override
    public String toString() {
        return value.orElse("");
    }
}