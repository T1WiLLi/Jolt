package ca.jolt.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a many-to-one relationship between entities.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ManyToOne {
    /**
     * (Optional) The field that owns the relationship.
     */
    String mappedBy() default "";

    /**
     * (Optional) Whether the fetch is eager (default) or lazy.
     */
    FetchType fetch() default FetchType.EAGER;
}