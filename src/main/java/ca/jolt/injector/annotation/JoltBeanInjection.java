package ca.jolt.injector.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark fields for dependency injection.
 * Optionally, a specific bean name can be provided; otherwise, the injection is
 * based on the field type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JoltBeanInjection {
    /**
     * Specifies the name of the bean to inject. If empty, the bean is resolved by
     * type.
     *
     * @return The bean name, or an empty string if type-based injection is used.
     */
    String value() default "";

    /**
     * Indicates whether the dependency is required. If true and the bean cannot be
     * injected, an exception is thrown.
     *
     * @return True if the dependency is required, false otherwise.
     */
    boolean required() default true;
}
