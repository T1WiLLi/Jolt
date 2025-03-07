package ca.jolt.security.policies;

import lombok.Getter;

/**
 * Defines policies for the {@code X-XSS-Protection} HTTP header.
 * <p>
 * Controls the browser's built-in XSS (Cross-Site Scripting) protection filter,
 * enabling or disabling it and specifying additional behavior like blocking or
 * reporting.
 * </p>
 */
public enum XssProtectionPolicy {
    /**
     * Disables the browser's XSS protection filter.
     */
    DISABLE("0"),

    /**
     * Enables the browser's XSS protection filter.
     */
    ENABLE("1"),

    /**
     * Enables the XSS protection filter and blocks the page if an attack is
     * detected.
     */
    ENABLE_BLOCK("1; mode=block"),

    /**
     * Enables the XSS protection filter with reporting (requires a report URI to be
     * specified).
     */
    ENABLE_REPORT("1; report=");

    /**
     * The string value of the policy as used in the HTTP header.
     */
    @Getter
    private final String value;

    /**
     * Constructs an XssProtectionPolicy with the specified header value.
     *
     * @param value The string representation of the policy.
     */
    XssProtectionPolicy(String value) {
        this.value = value;
    }
}