package ca.jolt.form;

import java.util.Map;
import java.util.function.BiPredicate;

/**
 * Represents a general validation rule for form fields. Classes implementing
 * this interface should define how to validate the field value based on
 * all available form data, and provide an error message for invalid cases.
 *
 * <p>
 * The {@link #validate(String, Map)} method is used by {@code Form} or
 * other components to determine if a field is valid, while
 * {@link #getErrorMessage()} provides the text to display if the validation
 * fails.
 * </p>
 *
 * @author William Beaudin
 * @since 1.0
 */
public interface Rule {

    /**
     * Validates the field value using the provided map of all form values.
     * <p>
     * Implementations typically check {@code data} (the current field's value) and
     * optionally other fields in {@code allValues} if cross-field checks are
     * needed.
     * </p>
     *
     * @param data
     *                  The field value to validate.
     * @param allValues
     *                  A map of all field values in the form, which may be used for
     *                  cross-field validations.
     * @return
     *         {@code true} if the field value passes validation;
     *         {@code false} otherwise.
     */
    boolean validate(String data, Map<String, String> allValues);

    /**
     * Returns a human-readable error message indicating why the validation
     * failed. This message is typically displayed if {@link #validate(String, Map)}
     * returns {@code false}.
     *
     * @return
     *         The error message to display on validation failure.
     */
    String getErrorMessage();

    /**
     * Factory method to create a new {@code Rule} based on a {@link BiPredicate}.
     * <p>
     * This allows quick, in-line creation of simple validation rules without
     * writing a separate class. The {@code predicate} should return {@code true}
     * if the data is valid, or {@code false} otherwise.
     * </p>
     *
     * @param predicate
     *                     A {@link BiPredicate} that takes the field value and the
     *                     map of
     *                     all form values, returning {@code true} for valid data or
     *                     {@code false} for invalid data.
     * @param errorMessage
     *                     A string representing the validation error message if the
     *                     predicate
     *                     fails.
     * @return
     *         A new {@code Rule} instance wrapping the given predicate and error
     *         message.
     */
    static Rule custom(BiPredicate<String, Map<String, String>> predicate, String errorMessage) {
        return new Rule() {
            @Override
            public boolean validate(String data, Map<String, String> allValues) {
                return predicate.test(data, allValues);
            }

            @Override
            public String getErrorMessage() {
                return errorMessage;
            }
        };
    }
}
