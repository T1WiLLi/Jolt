package io.github.t1willi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Version {

    /**
     * The version of the element to be exposed.
     * 
     * @return The version of this element.
     */
    int value();

    /**
     * The prefix for the version placeholder within the URL.
     * <p>
     * 
     * Example :
     * 
     * By default prefix is 'v', thus giving out something like : {@code '/v1/user'}
     * But, you can customize the prefix, to, for example 'version' :
     * {@code '/version1/user'}
     * 
     * @return The prefix for that version element.
     */
    String prefix() default "v";

}
