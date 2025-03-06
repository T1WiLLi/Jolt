package ca.jolt.security.policies;

import lombok.Getter;

public enum ReferrerPolicy {
    NO_REFERRER("no-referrer"),
    NO_REFERRER_WHEN_DOWNGRADE("no-referrer-when-downgrade"),
    SAME_ORIGIN("same-origin"),
    ORIGIN("origin"),
    STRICT_ORIGIN("strict-origin"),
    ORIGIN_WHEN_CROSS_ORIGIN("origin-when-cross-origin"),
    STRICT_ORIGIN_WHEN_CROSS_ORIGIN("strict-origin-when-cross-origin"),
    UNSAFE_URL("unsafe-url");

    @Getter
    private final String value;

    ReferrerPolicy(String value) {
        this.value = value;
    }
}
