package ca.jolt.form;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.function.Function;

import lombok.Getter;

/**
 * A class that encapsulates validation logic for a single form field.
 * <p>
 * Each {@code FieldValidator} is associated with exactly one field name in
 * a {@link ca.jolt.form.Form}. It maintains:
 * <ul>
 * <li>A list of {@link Rule} objects representing validation checks.</li>
 * <li>A list of {@link UnaryOperator} transformations that may alter or clean
 * the field value before validation (e.g., trimming whitespace, changing
 * case, etc.).</li>
 * <li>An optional condition {@link Predicate} that determines whether
 * validation should run at all, based on other form values.</li>
 * <li>An optional {@code valueType} (e.g., {@link Integer}, {@link Double},
 * {@link Boolean}), indicating how this field should be interpreted.</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * 
 * <pre>{@code
 * form.field("username")
 *         .required()
 *         .minLength(3)
 *         .toLowerCase()
 *         .when(allValues -> allValues.get("role").equals("user"));
 * }</pre>
 * <p>
 * When the {@link #verify()} method is called (typically
 * via the form’s {@link ca.jolt.form.Form#verify(String[])} or
 * {@link ca.jolt.form.Form#verifyAsync(String[])}),
 * the field's value undergoes the specified transformations, then
 * each registered {@code Rule} runs in order. If any rule fails,
 * an error is recorded in the parent form.
 *
 * @see ca.jolt.form.Form
 * @see BaseRules
 * @see Rule
 * @see AsyncRule
 * @author William
 * @since 1.0
 */
@Getter
public final class FieldValidator {

    /**
     * The name of the field this validator is associated with.
     */
    private final String fieldName;

    /**
     * A list of validation {@link Rule} instances associated with this field.
     */
    private final List<Rule> rules = new ArrayList<>();

    /**
     * A list of transformations applied to the field's value before validation.
     */
    private final List<UnaryOperator<String>> transformers = new ArrayList<>();

    /**
     * The parent {@link Form} that holds this validator and its field values.
     */
    private final Form form;

    /**
     * An optional condition that, when provided, decides if validation should
     * proceed. If this predicate returns {@code false}, validation is skipped
     * for this field.
     */
    private Predicate<Map<String, String>> condition;

    /**
     * An optional field for specifying the intended value type (e.g.,
     * {@link Integer}).
     */
    private Class<?> valueType;

    /**
     * Creates a validator for the given field name within the specified form.
     *
     * @param fieldName The name of the field to which this validator will apply
     * @param form      The parent {@link Form} managing field values and errors
     */
    public FieldValidator(String fieldName, Form form) {
        this.fieldName = fieldName;
        this.form = form;
    }

    /**
     * Adds multiple {@link Rule} instances to this field at once.
     *
     * @param rules One or more {@link Rule} objects
     * @return This {@code FieldValidator} instance (for fluent chaining)
     */
    public FieldValidator addRules(Rule... rules) {
        this.rules.addAll(Arrays.asList(rules));
        return this;
    }

    /**
     * Adds a single {@link Rule} to this field.
     *
     * @param rule The {@link Rule} to add
     * @return This {@code FieldValidator} instance (for fluent chaining)
     */
    public FieldValidator addRule(Rule rule) {
        rules.add(rule);
        return this;
    }

    /**
     * Specifies that this field must not be empty or {@code null}.
     *
     * @param errorMessage A custom error message if the field is empty or
     *                     {@code null}
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator required(String errorMessage) {
        addRule(BaseRules.required(errorMessage));
        return this;
    }

    /**
     * Specifies that this field must not be empty or {@code null}, using a
     * default error message.
     *
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator required() {
        return required("This field is required.");
    }

    /**
     * Specifies a minimum length for this field.
     *
     * @param min          The minimum number of characters required
     * @param errorMessage A custom error message
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator minLength(int min, String errorMessage) {
        addRule(BaseRules.minLength(min, errorMessage));
        return this;
    }

    /**
     * Specifies a minimum length, providing a default error message.
     *
     * @param min The minimum number of characters required
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator minLength(int min) {
        return minLength(min, "Value must be at least " + min + " characters long.");
    }

    /**
     * Specifies a maximum length for this field.
     *
     * @param max          The maximum number of characters allowed
     * @param errorMessage A custom error message
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator maxLength(int max, String errorMessage) {
        addRule(BaseRules.maxLength(max, errorMessage));
        return this;
    }

    /**
     * Specifies a maximum length, providing a default error message.
     *
     * @param max The maximum number of characters allowed
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator maxLength(int max) {
        return maxLength(max, "Value must be at most " + max + " characters long.");
    }

    /**
     * Specifies that this field must be a valid email address.
     *
     * @param errorMessage A custom error message on failure
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator email(String errorMessage) {
        addRule(BaseRules.email(errorMessage));
        return this;
    }

    /**
     * Specifies that this field must be a valid email address,
     * using a default error message.
     *
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator email() {
        return email("Invalid email format.");
    }

    /**
     * Specifies that this field may only contain letters and numbers.
     *
     * @param errorMessage A custom error message
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator alphanumeric(String errorMessage) {
        addRule(BaseRules.alphanumeric(errorMessage));
        return this;
    }

    /**
     * Specifies that this field may only contain letters and numbers,
     * using a default error message.
     *
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator alphanumeric() {
        return alphanumeric("Value must contain only letters and numbers.");
    }

    /**
     * Specifies that this field must match a valid phone number pattern.
     *
     * @param errorMessage A custom error message
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator phoneNumber(String errorMessage) {
        addRule(BaseRules.phoneNumber(errorMessage));
        return this;
    }

    /**
     * Specifies that this field must match a valid phone number pattern,
     * using a default error message.
     *
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator phoneNumber() {
        return phoneNumber("Invalid phone number format.");
    }

    /**
     * Specifies that this field must match a valid zip code pattern.
     *
     * @param errorMessage A custom error message
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator zipCode(String errorMessage) {
        addRule(BaseRules.zipCode(errorMessage));
        return this;
    }

    /**
     * Specifies that this field must match a valid zip code pattern,
     * using a default error message.
     *
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator zipCode() {
        return zipCode("Invalid zip code format.");
    }

    /**
     * Specifies that this field must match a valid URL pattern.
     *
     * @param errorMessage A custom error message
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator url(String errorMessage) {
        addRule(BaseRules.url(errorMessage));
        return this;
    }

    /**
     * Specifies that this field must match a valid URL pattern,
     * using a default error message.
     *
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator url() {
        return url("Invalid URL format.");
    }

    /**
     * Specifies that this field must be a valid credit card number
     * using the Luhn algorithm.
     *
     * @param errorMessage A custom error message
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator creditCard(String errorMessage) {
        addRule(BaseRules.creditCard(errorMessage));
        return this;
    }

    /**
     * Specifies that this field must be a valid credit card number,
     * using the Luhn algorithm, and a default error message.
     *
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator creditCard() {
        return creditCard("Invalid credit card number.");
    }

    /**
     * Specifies that this field must meet "strong password" criteria
     * (length, upper/lower, digit, special character).
     *
     * @param errorMessage A custom error message
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator strongPassword(String errorMessage) {
        addRule(BaseRules.strongPassword(errorMessage));
        return this;
    }

    /**
     * Specifies that this field must meet "strong password" criteria,
     * with a default error message.
     *
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator strongPassword() {
        return strongPassword("Password must be strong.");
    }

    /**
     * Specifies that this field must be a valid IP address (IPv4).
     *
     * @param errorMessage A custom error message
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator ipAddress(String errorMessage) {
        addRule(BaseRules.ipAddress(errorMessage));
        return this;
    }

    /**
     * Specifies that this field must be a valid IP address (IPv4),
     * with a default error message.
     *
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator ipAddress() {
        return ipAddress("Invalid IP address format.");
    }

    /**
     * Associates a custom date pattern for the field in the parent form,
     * and validates the field against that pattern.
     *
     * @param pattern The date format pattern (e.g., "yyyy-MM-dd")
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator date(String pattern) {
        form.registerDatePattern(fieldName, pattern);
        addRule(BaseRules.date(pattern, "Invalid date format. Use " + pattern + "."));
        return this;
    }

    /**
     * Associates a custom date pattern for the field in the parent form,
     * with a custom error message for invalid formats.
     *
     * @param pattern      The date format pattern (e.g., "yyyy-MM-dd")
     * @param errorMessage A custom error message if parsing fails
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator date(String pattern, String errorMessage) {
        form.registerDatePattern(fieldName, pattern);
        addRule(BaseRules.date(pattern, errorMessage));
        return this;
    }

    /**
     * Sets a condition that determines whether this validator should run.
     * If the condition evaluates to {@code false}, validation is skipped
     * for this field.
     *
     * @param condition A predicate accepting all field values to decide if
     *                  validation applies
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator when(Predicate<Map<String, String>> condition) {
        this.condition = condition;
        return this;
    }

    /**
     * Adds an asynchronous rule based on a provided function returning
     * a {@link CompletableFuture} of {@code Boolean}.
     *
     * @param asyncValidator The function that performs asynchronous validation
     * @param errorMessage   The error message if validation fails
     * @return This {@code FieldValidator} (for fluent chaining)
     * @see AsyncRule
     */
    public FieldValidator asyncRule(Function<String, CompletableFuture<Boolean>> asyncValidator, String errorMessage) {
        addRule(new AsyncRule(asyncValidator, errorMessage));
        return this;
    }

    /**
     * Flags this field's value for interpretation as an {@link Integer}.
     *
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator asInt() {
        this.valueType = Integer.class;
        return this;
    }

    /**
     * Flags this field's value for interpretation as a {@link Double}.
     *
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator asDouble() {
        this.valueType = Double.class;
        return this;
    }

    /**
     * Flags this field's value for interpretation as a {@link Boolean}.
     *
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator asBoolean() {
        this.valueType = Boolean.class;
        return this;
    }

    /**
     * Clamps the value to be within the specified minimum and maximum range.
     *
     * @param min The minimum value allowed
     * @param max The maximum value allowed
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator clamp(int min, int max) {
        clamp(min, max, fieldName);
        return this;
    }

    /**
     * Clamps the value to be within the specified minimum and maximum range.
     *
     * @param min          The minimum value allowed
     * @param max          The maximum value allowed
     * @param errorMessage A custom error message upon validation failure
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator clamp(int min, int max, String errorMessage) {
        asInt();
        addRule(BaseRules.clamp(min, max, errorMessage));
        return this;
    }

    /**
     * Specifies that the field must be greater than or equal to a given minimum
     * value, interpreting the field as a number.
     *
     * @param min          The minimum allowed numeric value
     * @param errorMessage A custom error message upon validation failure
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator min(int min, String errorMessage) {
        asInt();
        addRule(BaseRules.greaterEqualsThan(min, errorMessage));
        return this;
    }

    /**
     * Specifies that the field must be greater than or equal to a given minimum
     * value, using a default error message.
     *
     * @param min The minimum allowed numeric value
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator min(int min) {
        return min(min, "Value must be at least " + min + ".");
    }

    /**
     * Specifies that the field must be less than or equal to a given maximum value,
     * interpreting the field as a number.
     *
     * @param max          The maximum allowed numeric value
     * @param errorMessage A custom error message upon validation failure
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator max(int max, String errorMessage) {
        asInt();
        addRule(BaseRules.lowerEqualsThan(max, errorMessage));
        return this;
    }

    /**
     * Specifies that the field must be less than or equal to a given maximum value,
     * using a default error message.
     *
     * @param max The maximum allowed numeric value
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator max(int max) {
        return max(max, "Value must be at most " + max + ".");
    }

    /**
     * Adds a transformer to be applied on the field's value before validation.
     *
     * @param transformer A {@link UnaryOperator} that transforms the field value
     *                    (e.g., trimming)
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator transform(UnaryOperator<String> transformer) {
        this.transformers.add(transformer);
        return this;
    }

    /**
     * Trims leading and trailing whitespace from the field's value.
     *
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator trim() {
        return transform(String::trim);
    }

    /**
     * Converts the field's value to lowercase.
     *
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator toLowerCase() {
        return transform(String::toLowerCase);
    }

    /**
     * Converts the field's value to uppercase.
     *
     * @return This {@code FieldValidator} (for fluent chaining)
     */
    public FieldValidator toUpperCase() {
        return transform(String::toUpperCase);
    }

    /**
     * Verifies the current field's value against its own rules.
     * <p>
     * This method applies any registered transformers to the field value,
     * updates the value in the parent form, and then runs each attached rule.
     * If any rule fails, an error is added to the parent form and {@code false}
     * is returned; otherwise, {@code true} is returned.
     *
     * @return {@code true} if the field passes all validations; {@code false}
     *         otherwise
     */
    public boolean verify() {
        String value = form.getValue(fieldName);
        String transformedValue = value;
        for (UnaryOperator<String> transformer : transformers) {
            if (transformedValue != null) {
                transformedValue = transformer.apply(transformedValue);
            }
        }

        form.setValue(fieldName, transformedValue);
        for (Rule rule : rules) {
            if (!rule.validate(transformedValue)) {
                form.addError(fieldName, rule.getErrorMessage());
                return false;
            }
        }
        return true;
    }
}
