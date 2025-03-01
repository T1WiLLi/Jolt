package ca.jolt.form;

import java.util.Map;
import java.util.function.BiPredicate;

public interface Rule {
    /**
     * Validates the field value using the provided form values.
     * 
     * @param data      the field value to validate
     * @param allValues all form field values for cross-field validations
     * @return true if valid; false otherwise
     */
    boolean validate(String data, Map<String, String> allValues);

    /**
     * @return the error message to be used when validation fails.
     */
    String getErrorMessage();

    /**
     * Creates a new Rule using the given BiPredicate and error message.
     *
     * @param predicate    the predicate that tests the field value and the form
     *                     values.
     * @param errorMessage the error message if validation fails.
     * @return a new Rule instance.
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
