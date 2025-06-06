package io.github.t1willi.security.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NonceConfiguration {

    private final SecurityConfiguration parent;
    private boolean enabled;
    private final Set<String> excludedUrlPatterns;

    public NonceConfiguration(SecurityConfiguration parent) {
        this.parent = parent;
        this.enabled = false;
        this.excludedUrlPatterns = new HashSet<>();
    }

    public SecurityConfiguration and() {
        return parent;
    }

    public NonceConfiguration enable() {
        this.enabled = true;
        return this;
    }

    public NonceConfiguration excludeUrlPatterns(String... excludedUrls) {
        Collections.addAll(this.excludedUrlPatterns, excludedUrls);
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Set<String> getExcludedUrlPatterns() {
        return Collections.unmodifiableSet(excludedUrlPatterns);
    }
}
