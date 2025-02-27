package ca.jolt.injector.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark fields for dependency injection.
 * Optionnaly, a specific bean name can be provided; otherwise, the injection is based on the field type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JoltBeanInjection {
    String value() default "";

    boolean required() default true;
}
