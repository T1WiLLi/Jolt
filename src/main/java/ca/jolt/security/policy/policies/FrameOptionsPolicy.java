package ca.jolt.security.policy.policies;

import lombok.Getter;

public enum FrameOptionsPolicy {
    DENY("DENY"),
    SAME_ORIGIN("SAMEORIGIN"),
    ALLOW_FROM("ALLOW-FROM ");

    @Getter
    private final String value;

    FrameOptionsPolicy(String value) {
        this.value = value;
    }
}
