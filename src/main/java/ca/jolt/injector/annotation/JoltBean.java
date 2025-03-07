package ca.jolt.injector.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ca.jolt.injector.type.BeanScope;
import ca.jolt.injector.type.InitializationMode;

/**
 * Annotation to mark a class as a bean to be managed by the JoltContainer.
 * The bean can specify an optional name, it's scope (singleton or prototype),
 * and whether it should be initialized eagerly or lazily.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JoltBean {
    /**
     * Optional bean name. If empty, the name is derived from the class name.
     * 
     * @return the bean name
     */
    String value() default "";

    /**
     * The scope of the bean.
     * 
     * @return the scope
     */
    BeanScope scope() default BeanScope.SINGLETON;

    /**
     * Whether the bean should be initialized eagerly or lazily.
     * 
     * @return the initialization mode
     */
    InitializationMode initialization() default InitializationMode.EAGER;
}
