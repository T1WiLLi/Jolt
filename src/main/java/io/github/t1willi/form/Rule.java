package io.github.t1willi.form;

import java.util.function.Predicate;

/**
 * Represents a general validation rule for form fields. Classes implementing
 * this interface should define how to validate the field value based on
 * all available form data, and provide an error message for invalid cases.
 * <p>
 * The {@link #validate(String)} method is used by {@code Form} or
 * other components to determine if a field is valid, while
 * {@link #getErrorMessage()} provides the text to display if the validation
 * fails.
 * 
 * <p>
 * Note: The actual method signature in this interface is
 * {@link #validate(String)}, which performs the validation check on the
 * field value alone. Implementations may choose to reference additional
 * data as needed.
 * 
 * @author William
 * @since 1.0
 */
public interface Rule {

    /**
     * Validates the field value.
     * <p>
     * Implementations typically check {@code data} (the current field's value).
     * </p>
     *
     * @param data The field value to validate
     * @return {@code true} if the field value passes validation;
     *         {@code false} otherwise
     */
    boolean validate(String data);

    /**
     * Returns a human-readable error message indicating why the validation
     * failed. This message is typically displayed if {@link #validate(String)}
     * returns {@code false}.
     *
     * @return The error message to display on validation failure
     */
    String getErrorMessage();

    /**
     * Factory method to create a new {@code Rule} based on a {@link Predicate}.
     * <p>
     * This allows quick, in-line creation of simple validation rules without
     * writing a separate class. The {@code predicate} should return {@code true}
     * if the data is valid, or {@code false} otherwise.
     * </p>
     *
     * @param predicate    A {@link Predicate} that takes the field value,
     *                     returning {@code true} for valid data or {@code false}
     *                     for invalid data
     * @param errorMessage A string representing the validation error message if
     *                     the predicate fails
     * @return A new {@code Rule} instance wrapping the given predicate and error
     *         message
     */
    static Rule custom(Predicate<String> predicate, String errorMessage) {
        return new Rule() {
            @Override
            public boolean validate(String data) {
                return predicate.test(data);
            }

            @Override
            public String getErrorMessage() {
                return errorMessage;
            }
        };
    }
}