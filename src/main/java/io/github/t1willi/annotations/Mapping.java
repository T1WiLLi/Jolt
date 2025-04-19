package io.github.t1willi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.t1willi.http.HttpMethod;

/**
 * <strong>Warning</strong>: This annotation, altought it's possible to use on a
 * class, it won't work, The annotation is designed to be used on methods, and
 * is available for use on class only for internal purposes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Mapping {
    /**
     * The HTTP method to use for the mapping.
     * 
     * @return The HTTP method of the mapping.
     */
    HttpMethod method() default HttpMethod.GET;

    /**
     * The path of the mapping.
     * 
     * @return The path of the mapping.
     */
    String value() default "/";
}
