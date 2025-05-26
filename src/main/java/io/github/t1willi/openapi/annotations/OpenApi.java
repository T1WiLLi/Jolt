package io.github.t1willi.openapi.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OpenApi {
    String title() default "Jolt - OpenAPI";

    String version() default "1.0.0";

    String description() default "Jolt x OpenAPI: API Documentation";

    String termsOfService() default "";

    String contactName() default "";

    String contactEmail() default "";

    String contactUrl() default "";

    String licenseName() default "Apache-2.0";

    String licenseUrl() default "https://www.apache.org/licenses/LICENSE-2.0.html";

    String path() default "/openapi.json";
}
