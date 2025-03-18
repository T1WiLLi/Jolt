package ca.jolt.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a many-to-many relationship between entities.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ManyToMany {
    /**
     * The field in the target entity that maps back to this entity.
     */
    String mappedBy() default "";

    /**
     * Whether the fetch is eager or lazy (default).
     */
    FetchType fetch() default FetchType.LAZY;

    /**
     * Cascade operations to apply to related entities.
     */
    CascadeType[] cascade() default {};

    /**
     * The name of the join table. If not specified, it will be generated as:
     * [ownerTable]_[inverseTable]
     */
    String joinTable() default "";

    /**
     * The name of the foreign key column in the join table referencing the owner
     * entity.
     */
    String joinColumn() default "";

    /**
     * The name of the foreign key column in the join table referencing the inverse
     * entity.
     */
    String inverseJoinColumn() default "";
}