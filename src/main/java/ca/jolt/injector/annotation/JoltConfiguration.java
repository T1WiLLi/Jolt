package ca.jolt.injector.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ca.jolt.injector.type.ConfigurationType;

/**
 * Annotation to mark a configuration class.
 * When scanned, the container will instantiate the configuration class and
 * register it under the specified configuration type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JoltConfiguration {
    /**
     * The type of configuration. For example, EXCEPTION_HANDLER, SECURITY, SERVER,
     * etc.
     * 
     */
    ConfigurationType value();
}
