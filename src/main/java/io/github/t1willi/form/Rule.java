package io.github.t1willi.form;

import java.util.function.Predicate;

/**
 * An interface for defining validation rules for form field data.
 * <p>
 * The {@code Rule} interface provides a contract for validating form field
 * values and retrieving
 * associated error messages. It is designed to be used in conjunction with the
 * {@link Field} interface
 * to apply validation logic to form fields in a web application.
 * Implementations of this interface
 * encapsulate a specific validation rule, such as checking if a value matches a
 * pattern or satisfies
 * a custom condition. The static {@link #custom(Predicate, String)} method
 * provides a convenient way
 * to create rule instances with custom validation logic using a
 * {@link Predicate}. This interface is
 * typically used to modularize and reuse validation logic across form fields.
 *
 * @since 1.0.0
 */
public interface Rule {

    /**
     * Validates the provided data against this rule.
     * <p>
     * This method evaluates the given data string against the validation logic
     * defined by this rule.
     * It returns {@code true} if the data satisfies the rule, and {@code false}
     * otherwise. The
     * specific validation logic depends on the implementation, which may check for
     * patterns, ranges,
     * or custom conditions.
     *
     * @param data the data string to validate
     * @return {@code true} if the data is valid according to this rule,
     *         {@code false} otherwise
     * @throws IllegalArgumentException if the data is null and the rule does not
     *                                  allow null values
     * @since 1.0.0
     */
    boolean validate(String data);

    /**
     * Retrieves the error message associated with this rule.
     * <p>
     * This method returns the error message to be used when the validation
     * performed by {@link #validate(String)}
     * fails. The error message provides a human-readable description of the
     * validation failure, suitable
     * for display in a user interface or for logging purposes.
     *
     * @return the error message associated with this rule
     * @since 1.0.0
     */
    String getErrorMessage();

    /**
     * Creates a custom validation rule using a predicate and an error message.
     * <p>
     * This static factory method constructs a {@link Rule} instance that validates
     * data using the
     * provided {@link Predicate}. If the predicate returns {@code false} for the
     * data, the specified
     * error message is used to indicate the validation failure. This method is
     * useful for defining
     * custom validation logic that can be applied to form fields in a flexible and
     * reusable manner.
     *
     * @param predicate    the {@link Predicate} used to validate the data
     * @param errorMessage the error message to use if the predicate returns
     *                     {@code false}
     * @return a new {@link Rule} instance implementing the custom validation logic
     * @throws IllegalArgumentException if the predicate or error message is null
     * @since 1.0.0
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