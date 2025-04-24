package io.github.t1willi.exceptions.handler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods as exception handlers.
 * Methods annotated with @HandleException will be automatically registered
 * for the specified exception types at startup.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface HandleException {

    /**
     * One or more exception classes that this method handles.
     */
    Class<? extends Throwable>[] value() default Throwable.class;
}
