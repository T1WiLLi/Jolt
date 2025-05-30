package io.github.t1willi.security.authentification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface Authorize {
    Class<? extends AuthStrategy> strategy() default SessionAuthStrategy.class;

    String onFailure() default "";
}
