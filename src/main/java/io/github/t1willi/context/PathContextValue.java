package io.github.t1willi.context;

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
    public PathContextValue(String value) {
        super(value);
    }
}