package ca.jolt.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use this annotation on String fields to define an enumerated type with
 * allowed values.
 * Example:
 *
 * <pre>
 * &#64;Enum(values = { "ACTIVE", "INACTIVE", "PENDING" })
 * &#64;Column(nullable = false, length = 50)
 * private String status;
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CheckEnum {
    String[] values();
}