package io.github.t1willi.annotations;

import java.lang.annotation.*;

/**
 * Binds a method parameter to a JSON body field or to the entire body object.
 * <p>
 * If {@code value()} is non‑empty, the framework will parse the incoming JSON
 * into a Map and extract that single key’s value, converting it to the
 * parameter type. Otherwise, the parameter will be deserialized in full
 * into the given type.
 * </p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestBody {
    /**
     * The name of the JSON property to bind. Leave blank to deserialize
     * the entire request body into the parameter’s type.
     */
    String value() default "";
}
