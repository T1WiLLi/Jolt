package ca.jolt.security.policies;

import lombok.Getter;

/**
 * Defines policies for the {@code Referrer-Policy} HTTP header.
 * <p>
 * Controls how much referrer information (sent via the Referer header) should
 * be
 * included with requests, enhancing privacy and security.
 * </p>
 */
public enum ReferrerPolicy {
    /**
     * Sends no referrer information with requests.
     */
    NO_REFERRER("no-referrer"),

    /**
     * Sends referrer information only for same-origin requests; none for downgrades
     * (HTTPS to HTTP).
     */
    NO_REFERRER_WHEN_DOWNGRADE("no-referrer-when-downgrade"),

    /**
     * Sends referrer information only for same-origin requests.
     */
    SAME_ORIGIN("same-origin"),

    /**
     * Sends only the origin (not the full URL) as referrer information.
     */
    ORIGIN("origin"),

    /**
     * Sends only the origin for same-origin or HTTPS requests.
     */
    STRICT_ORIGIN("strict-origin"),

    /**
     * Sends the full URL for same-origin requests, only the origin for
     * cross-origin.
     */
    ORIGIN_WHEN_CROSS_ORIGIN("origin-when-cross-origin"),

    /**
     * Sends the full URL for same-origin or HTTPS requests, only the origin
     * otherwise.
     */
    STRICT_ORIGIN_WHEN_CROSS_ORIGIN("strict-origin-when-cross-origin"),

    /**
     * Sends the full URL as referrer information, regardless of security or origin.
     */
    UNSAFE_URL("unsafe-url");

    /**
     * The string value of the policy as used in the HTTP header.
     */
    @Getter
    private final String value;

    /**
     * Constructs a ReferrerPolicy with the specified header value.
     *
     * @param value The string representation of the policy.
     */
    ReferrerPolicy(String value) {
        this.value = value;
    }
}