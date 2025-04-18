package io.github.t1willi.schedule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Scheduled {
    long fixed() default -1;

    TimeUnit timeUnit() default TimeUnit.MILLISECONDS;

    long initialDelay() default 0;

    String cron() default "";
}
