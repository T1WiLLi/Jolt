package ca.jolt.routing.context;

public final class PathContextValue extends JoltHttpContextValue {

    public PathContextValue(String value) {
        super(value);
    }

    public int asInt() {
        if (!isPresent()) {
            throw new IllegalStateException("value is not present");
        }

        try {
            return Integer.parseInt(get());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("value is not an integer", e);
        }
    }

    public double asDouble() {
        if (!isPresent()) {
            throw new IllegalStateException("value is not present");
        }

        try {
            return Double.parseDouble(get());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("value is not a double", e);
        }
    }
}
