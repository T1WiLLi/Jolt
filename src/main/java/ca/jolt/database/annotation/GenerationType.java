package ca.jolt.database.annotation;

/**
 * Enum defining the available ID generation strategies.
 */
public enum GenerationType {
    /**
     * Relies on database auto-increment functionality
     */
    IDENTITY,

    /**
     * Uses UUID generation for unique IDs
     */
    UUID,

    /**
     * Uses sequence-based ID generation with optional prefix
     */
    SEQUENCE
}
