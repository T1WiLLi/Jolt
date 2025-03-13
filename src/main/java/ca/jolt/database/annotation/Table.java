package ca.jolt.database.annotation;

import java.lang.annotation.*;

/**
 * Annotation to specify the database table for an entity.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {
    String table();
}
