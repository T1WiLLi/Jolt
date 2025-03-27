package io.github.security.policies;

import lombok.Getter;

public enum CacheControlPolicy {
    /**
     * Prevents caching entierly.
     */
    NO_CACHE("no-cache, no-store, must-revalidate"),

    /**
     * Allows public caching for a specified time (1 hour).
     */
    PUBLIC_MAX_AGE("public, max-age=3600"),

    /**
     * Indicates that the response is private.
     */
    PRIVATE("private, max-age=0, no-transform");

    @Getter
    private final String value;

    /**
     * Construct a new CacheControlPolicy.
     * 
     * @param value
     */
    CacheControlPolicy(String value) {
        this.value = value;
    }
}
