package ca.jolt.database.annotation;

/**
 * Defines the fetching strategy to use for a relationship.
 */
public enum FetchType {
    /**
     * Fetch data eagerly, when the parent entity is loaded.
     */
    EAGER,

    /**
     * Fetch data lazily, only when actually accessed.
     */
    LAZY
}