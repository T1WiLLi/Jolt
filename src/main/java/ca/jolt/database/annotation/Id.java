package ca.jolt.database.annotation;

import java.lang.annotation.*;

/**
 * Annotation to mark a field as the primary key.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Id {
    // In the future, you can add attributes for generation strategy.
}
