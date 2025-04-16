package io.github.t1willi.security.config;

public class NonceConfiguration {

    private final SecurityConfiguration parent;

    private boolean enabled;

    public NonceConfiguration(SecurityConfiguration parent) {
        this.parent = parent;
        this.enabled = false;
    }

    public SecurityConfiguration and() {
        return parent;
    }

    public NonceConfiguration enable() {
        this.enabled = true;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
