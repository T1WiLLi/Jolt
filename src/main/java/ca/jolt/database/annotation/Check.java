package ca.jolt.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Check {
    /**
     * SQL condition for the CHECK constraint. Use '?' as a placeholder for the
     * column name.
     */
    String condition();

    /**
     * Optional message for validation errors.
     */
    String message() default "";
}
