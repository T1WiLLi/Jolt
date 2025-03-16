package ca.jolt.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
    /**
     * The name of the database column
     * 
     * @return The column name
     */
    String value() default "";

    /**
     * Maximum length for string columns
     * 
     * @return The maximum length
     */
    int length() default 255;

    /**
     * Whether the column can contain NULL values
     * 
     * @return True if nullable, false otherwise
     */
    boolean nullable() default true;
}
