package io.github.t1willi.openapi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Docs {
    String summary();

    String description() default "";

    String[] tags() default {};

    Class<?> requestBody() default Void.class;

    String requestDescription() default "";

    ApiResponse[] responses() default {
            @ApiResponse(code = 200, description = "Success")
    };

    ApiParameter[] parameters() default {};

    String[] security() default {};

    boolean deprecated() default false;

    String operationId() default "";

}