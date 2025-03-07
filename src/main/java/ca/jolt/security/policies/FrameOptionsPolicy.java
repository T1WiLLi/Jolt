package ca.jolt.security.policies;

import lombok.Getter;

/**
 * Defines policies for the {@code X-Frame-Options} HTTP header.
 * <p>
 * Specifies whether a browser should be allowed to render a page in a frame,
 * iframe, or object, helping to prevent clickjacking attacks.
 * </p>
 */
public enum FrameOptionsPolicy {
    /**
     * Prevents the page from being displayed in a frame, regardless of origin.
     */
    DENY("DENY"),

    /**
     * Allows the page to be displayed in a frame only if the frame is from the same
     * origin.
     */
    SAME_ORIGIN("SAMEORIGIN"),

    /**
     * Allows the page to be displayed in a frame from a specified URI (requires
     * additional configuration).
     */
    ALLOW_FROM("ALLOW-FROM ");

    /**
     * The string value of the policy as used in the HTTP header.
     */
    @Getter
    private final String value;

    /**
     * Constructs a FrameOptionsPolicy with the specified header value.
     *
     * @param value The string representation of the policy.
     */
    FrameOptionsPolicy(String value) {
        this.value = value;
    }
}