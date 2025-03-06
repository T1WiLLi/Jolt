package ca.jolt.security.policy.policies;

import lombok.Getter;

public enum HstsPolicy {
    ONE_YEAR("max-age=31536000"),
    ONE_YEAR_WITH_SUBDOMAINS("max-age=31536000; includeSubDomains"),
    ONE_YEAR_WITH_SUBDOMAINS_PRELOAD("max-age=31536000; includeSubDomains; preload"),
    SIX_MONTHS("max-age=15768000"),
    SIX_MONTHS_WITH_SUBDOMAINS("max-age=15768000; includeSubDomains"),
    ONE_MONTH("max-age=2592000");

    @Getter
    private final String value;

    HstsPolicy(String value) {
        this.value = value;
    }
}
