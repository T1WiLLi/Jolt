package ca.jolt.database.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JoinTable {
    /**
     * Name of the join table.
     * If empty, the framework will generate a default name.
     */
    String name() default "";

    /**
     * The join columns that reference the owner of the relationship.
     * Typically the columns referencing 'this' entity's primary key.
     */
    JoinColumn[] joinColumns() default {};

    /**
     * The inverse join columns that reference the other side of the relationship.
     */
    JoinColumn[] inverseJoinColumns() default {};
}
