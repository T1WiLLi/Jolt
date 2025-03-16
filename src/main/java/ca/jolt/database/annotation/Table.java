package ca.jolt.database.annotation;

import java.lang.annotation.*;

/**
 * Annotation to specify the database table for an entity.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {
    /**
     * The name of the database table
     * 
     * @return The table name
     */
    String table();

    /**
     * Array of column names that should have unique constraints
     * 
     * @return Unique column names
     */
    String[] unique() default {};

    /**
     * Array of column names that should be indexed
     * 
     * @return Indexed column names
     */
    String[] indexes() default {};
}
