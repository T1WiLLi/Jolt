package io.github.t1willi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.t1willi.http.HttpMethod;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Mapping(method = HttpMethod.POST)
public @interface Post {
    String path() default "/";
}
