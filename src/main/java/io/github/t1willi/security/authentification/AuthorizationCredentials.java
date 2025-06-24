package io.github.t1willi.security.authentification;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify expected credentials for authorization.
 * This annotation can be applied to methods or classes to define
 * the expected credentials for authorization checks.
 * It allows specifying multiple keys and their expected values,
 * as well as the expected types of those values.
 * 
 * It will automatically work with Jolt's default AuthStrategy,
 * which are SessionAuthStrategy, and JWTAuthStrategy.
 * 
 * <p>
 * Example usage:
 * 
 * <pre>
 * {@code
 * &#64;AuthorizationCredentials(
 *     key = {"username", "password"},
 *     expected = {"admin", "secret"},
 *     expectedTypes = {String.class, String.class}
 * )
 * public void secureMethod() {
 *     // Method implementation
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface AuthorizationCredentials {
    String[] key();

    String[] expected();

    Class<?>[] expectedTypes() default {};
}