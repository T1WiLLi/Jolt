package ca.jolt.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a one-to-many relationship between entities.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OneToMany {
    /**
     * The field in the target entity that maps back to this entity.
     */
    String mappedBy();

    /**
     * Whether the fetch is eager or lazy (default).
     */
    FetchType fetch() default FetchType.LAZY;

    /**
     * Cascade operations to apply to related entities.
     */
    CascadeType[] cascade() default {};

    /**
     * Whether to automatically remove orphaned entities.
     */
    boolean orphanRemoval() default false;
}