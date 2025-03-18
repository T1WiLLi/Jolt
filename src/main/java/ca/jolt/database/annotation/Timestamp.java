package ca.jolt.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for fields that should be automatically populated with timestamps.
 * Can be used to mark fields that should store creation time or update time.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Timestamp {
    /**
     * Specifies when this timestamp should be updated.
     */
    TimestampType forUpdate();

    /**
     * Timestamp types available.
     */
    public enum TimestampType {
        /**
         * Set only when entity is first created.
         */
        CREATED_AT,

        /**
         * Set when entity is created and updated.
         */
        UPDATED_AT
    }
}
