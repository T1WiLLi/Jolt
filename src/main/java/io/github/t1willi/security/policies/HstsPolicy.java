package io.github.t1willi.security.policies;

import lombok.Getter;

/**
 * Defines policies for the {@code Strict-Transport-Security} (HSTS) HTTP
 * header.
 * <p>
 * Enforces the use of HTTPS for a specified duration, optionally including
 * subdomains
 * or enabling preloading in browsers.
 * </p>
 */
public enum HstsPolicy {
    /**
     * Enforces HTTPS for one year (31,536,000 seconds).
     */
    ONE_YEAR("max-age=31536000"),

    /**
     * Enforces HTTPS for one year, including all subdomains.
     */
    ONE_YEAR_WITH_SUBDOMAINS("max-age=31536000; includeSubDomains"),

    /**
     * Enforces HTTPS for one year, including subdomains, with HSTS preload
     * eligibility.
     */
    ONE_YEAR_WITH_SUBDOMAINS_PRELOAD("max-age=31536000; includeSubDomains; preload"),

    /**
     * Enforces HTTPS for six months (15,768,000 seconds).
     */
    SIX_MONTHS("max-age=15768000"),

    /**
     * Enforces HTTPS for six months, including all subdomains.
     */
    SIX_MONTHS_WITH_SUBDOMAINS("max-age=15768000; includeSubDomains"),

    /**
     * Enforces HTTPS for one month (2,592,000 seconds).
     */
    ONE_MONTH("max-age=2592000");

    /**
     * The string value of the policy as used in the HTTP header.
     */
    @Getter
    private final String value;

    /**
     * Constructs an HstsPolicy with the specified header value.
     *
     * @param value The string representation of the policy.
     */
    HstsPolicy(String value) {
        this.value = value;
    }
}