package ca.jolt.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a column for joining an entity association.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JoinColumn {
    /**
     * The name of the foreign key column.
     */
    String value();

    /**
     * Whether the column can contain NULL values.
     */
    boolean nullable() default true;

    /**
     * The name of the referenced column in the target entity table.
     */
    String referencedColumnName() default "";
}