package io.github.t1willi.openapi.annotations;

public @interface ApiParameter {

    public static enum In {
        PATH, QUERY, HEADER, COOKIE
    }

    String name();

    In in();

    String description() default "";

    boolean required() default false;

    Class<?> type() default String.class;

    String example() default "";
}
