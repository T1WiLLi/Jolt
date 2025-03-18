package ca.jolt.database.annotation;

/**
 * Defines the cascade operations that can be applied to related entities.
 */
public enum CascadeType {
    /**
     * Cascade all operations.
     */
    ALL,

    /**
     * Cascade persist operations.
     */
    PERSIST,

    /**
     * Cascade merge operations.
     */
    MERGE,

    /**
     * Cascade remove operations.
     */
    REMOVE,

    /**
     * Cascade refresh operations.
     */
    REFRESH,

    /**
     * Cascade detach operations.
     */
    DETACH
}