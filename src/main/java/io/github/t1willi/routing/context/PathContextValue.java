package io.github.t1willi.routing.context;

/**
 * A specialized {@link JoltHttpContextValue} representing a path parameter.
 * It provides methods for type conversion from string to integer or double.
 * <p>
 * Example usage:
 * 
 * <pre>{@code
 * int userId = ctx.path("id").asInt();
 * }</pre>
 *
 * @see JoltContext
 * @see JoltHttpContextValue
 * @author William Beaudin
 * @since 1.0
 */
public final class PathContextValue extends JoltHttpContextValue {

    /**
     * Constructs a new {@link PathContextValue}, wrapping the provided string.
     *
     * @param value
     *              A path parameter value, potentially null.
     */
    public PathContextValue(String value) {
        super(value);
    }

    /**
     * Converts the underlying value to an integer.
     *
     * @return
     *         The integer representation of the path value.
     * @throws IllegalStateException
     *                               If the value is absent or not a valid integer.
     */
    public int asInt() {
        if (!isPresent()) {
            throw new IllegalStateException("value is not present");
        }
        try {
            return Integer.parseInt(this.toString());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("value is not an integer", e);
        }
    }

    /**
     * Converts the underlying value to a double.
     *
     * @return
     *         The double representation of the path value.
     * @throws IllegalStateException
     *                               If the value is absent or not a valid double.
     */
    public double asDouble() {
        if (!isPresent()) {
            throw new IllegalStateException("value is not present");
        }
        try {
            return Double.parseDouble(this.toString());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("value is not a double", e);
        }
    }
}