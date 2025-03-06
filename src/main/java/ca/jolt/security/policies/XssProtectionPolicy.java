package ca.jolt.security.policy.policies;

import lombok.Getter;

public enum XssProtectionPolicy {
    DISABLE("0"),
    ENABLE("1"),
    ENABLE_BLOCK("1; mode=block"),
    ENABLE_REPORT("1; report=");

    @Getter
    private final String value;

    XssProtectionPolicy(String value) {
        this.value = value;
    }
}
