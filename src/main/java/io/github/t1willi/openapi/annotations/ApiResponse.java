package io.github.t1willi.openapi.annotations;

public @interface ApiResponse {
    int code();

    String description();

    Class<?> schema() default Void.class;

    String contentType() default "application/json";
}
