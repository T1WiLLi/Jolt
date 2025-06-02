package io.github.t1willi.injector.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a configuration class.
 * When scanned, the container will instantiate the configuration class and
 * register it under the specified configuration type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Configuration {

    /**
     * Flag to indicate if this configuration is the framework's default
     * implementation.
     * User provided configurations should have this set to false.
     * 
     * @return true if this is the default implementation, false otherwise
     */
    boolean isDefault() default false;
}