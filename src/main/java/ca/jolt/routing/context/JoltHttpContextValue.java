package ca.jolt.routing.context;

import java.util.Optional;

public abstract class JoltHttpContextValue {
    protected final Optional<String> value;

    public JoltHttpContextValue(String value) {
        this.value = Optional.ofNullable(value);
    }

    public boolean isPresent() {
        return value.isPresent();
    }

    public String get() {
        return value.orElseThrow(() -> new IllegalStateException("The value is not present."));
    }
}
