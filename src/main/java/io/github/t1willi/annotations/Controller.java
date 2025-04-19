package io.github.t1willi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Controller {

    // The default should be the controller name, we will follow ASP C# Convetion
    // Example : @Controller("[controller]") for a controller named UserController,
    // then
    // the root would be "/user";
    /**
     * The root path of the controller.
     * 
     * @return The root path of the controller
     */
    String value() default "";
}
