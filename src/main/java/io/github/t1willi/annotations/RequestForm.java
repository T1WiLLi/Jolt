package io.github.t1willi.annotations;

import java.lang.annotation.*;

/**
 * Binds a method parameter to a form field or to the entire form.
 * <p>
 * If {@code value()} is non‑empty, the framework will extract that single
 * field from the submitted form and convert it to the parameter type.
 * Otherwise, the parameter type must be {@link io.github.t1willi.form.Form
 * Form},
 * and the entire form will be injected.
 * </p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestForm {
    /**
     * The name of the form field to bind. Leave blank to inject the
     * {@link io.github.t1willi.form.Form whole form} object.
     */
    String value() default "";
}
