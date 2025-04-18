package io.github.t1willi.form;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.t1willi.exceptions.FormConversionException;
import io.github.t1willi.template.JoltModel;
import io.github.t1willi.utils.JacksonUtil;
import lombok.ToString;

/**
 * A container for form fields, field values, and their associated validators.
 * <p>
 * This class orchestrates the validation process—both synchronous and
 * asynchronous—by delegating to each field's {@link FieldValidator}.
 * <p>
 * <strong>Key responsibilities:</strong>
 * <ul>
 * <li>Store raw field values in a map.</li>
 * <li>Maintain a map of field names to {@link FieldValidator} instances.</li>
 * <li>Perform synchronous validation via {@link #verify(String[])}.</li>
 * <li>Perform asynchronous validation (supporting {@link AsyncRule}) via
 * {@link #verifyAsync(String[])}.</li>
 * <li>Track validation errors in a dedicated map.</li>
 * <li>Check for field existence and values.</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * 
 * <pre>{@code
 *
 * Form form = new Form(); // You can also do: ctx.buildForm();
 * form.setValue("username", "john_doe");
 *
 * form.field("username")
 *         .required("Username is required")
 *         .minLength(3, "Username must be at least 3 characters long.");
 *
 * if (form.verify()) {
 *     // All validations passed!
 * } else {
 *     // Handle errors
 *     Map<String, String> errors = form.getErrors();
 * }
 * }</pre>
 *
 * @see FieldValidator
 * @see Rule
 * @see AsyncRule
 * @author William
 * @since 1.0
 */
@ToString
public final class Form {

    private static final Logger logger = Logger.getLogger(Form.class.getName());
    private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";

    /**
     * Maps each field name to its current string value.
     */
    private final Map<String, String> fieldValues = new LinkedHashMap<>();

    /**
     * Maps each field name to its associated {@link FieldValidator}.
     */
    private final Map<String, FieldValidator> fieldValidators = new LinkedHashMap<>();

    /**
     * Stores all error messages for fields that fail validation.
     * Each field can have multiple error messages (one for each failed rule).
     */
    private final Map<String, List<String>> allErrors = new LinkedHashMap<>();

    /**
     * Stores the first error message for each field that fails validation.
     * This is used for backward compatibility with code that expects only one error
     * per field.
     */
    private final Map<String, String> firstErrors = new LinkedHashMap<>();

    /**
     * Holds any custom date patterns for fields, used in date
     * conversions/validations.
     */
    private final Map<String, String> datePatterns = new HashMap<>();

    /**
     * A template used when constructing all error messages via
     * {@link #getAllErrors()}.
     * <p>
     * {@code {field}} is replaced by the field name, and {@code {message}}
     * is replaced by the validation error message.
     */
    private String errorTemplate = "{field}: {message}";

    /**
     * An optional callback that runs if the form passes all validations.
     */
    private Runnable successCallback;

    /**
     * Constructs a new {@code Form} with optional initial field data.
     *
     * @param initialData A map of initial field data, or {@code null} for an empty
     *                    form
     */
    public Form(Map<String, String> initialData) {
        if (initialData != null) {
            fieldValues.putAll(initialData);
        }
    }

    /**
     * Constructs a new, empty {@code Form} (no initial data).
     */
    public Form() {
        this(null);
    }

    /**
     * Retrieves (or creates) a {@link FieldValidator} for the specified field name.
     *
     * @param fieldName The name of the field to be validated
     * @return The associated {@link FieldValidator}
     */
    public FieldValidator field(String fieldName) {
        fieldValidators.computeIfAbsent(fieldName, name -> new FieldValidator(name, this));
        return fieldValidators.get(fieldName);
    }

    /**
     * Sets (or updates) the value for the specified field in this form.
     *
     * @param fieldName The field name
     * @param value     The field value as a string
     * @return This {@code Form} object (for fluent chaining)
     */
    public Form setValue(String fieldName, String value) {
        fieldValues.put(fieldName, value);
        return this;
    }

    /**
     * Checks if the form contains a field with the specified name.
     * A field exists if it has been added to the fieldValues map or has a
     * validator.
     *
     * @param fieldName The name of the field to check
     * @return {@code true} if the field exists; {@code false} otherwise
     */
    public boolean hasField(String fieldName) {
        return fieldValues.containsKey(fieldName) || fieldValidators.containsKey(fieldName);
    }

    /**
     * Checks if the specified field has a non-null, non-empty value.
     *
     * @param fieldName The name of the field to check
     * @return {@code true} if the field has a value; {@code false} if the field
     *         doesn't exist or has a null/empty value
     */
    public boolean hasValue(String fieldName) {
        String value = fieldValues.get(fieldName);
        return value != null && !value.isEmpty();
    }

    /**
     * Verifies all fields in this form, except those specified in the
     * {@code excludedFields} array.
     * <p>
     * Any fields whose names are included in {@code excludedFields} will be
     * skipped and assumed valid. The method logs the verification results and,
     * if validation passes and a success callback is registered, it executes
     * the callback.
     *
     * @param excludedFields Field names to skip during validation
     * @return {@code true} if all non-excluded fields pass validation;
     *         {@code false} otherwise
     */
    public boolean verify(String... excludedFields) {
        clearErrors();
        boolean isValid = true;
        Set<String> excludeSet = new HashSet<>(Arrays.asList(excludedFields));

        for (Map.Entry<String, FieldValidator> entry : fieldValidators.entrySet()) {
            if (excludeSet.contains(entry.getKey())) {
                continue;
            }
            if (!entry.getValue().verify()) {
                isValid = false;
            }
        }

        logVerificationResults(isValid);

        if (isValid && successCallback != null) {
            successCallback.run();
        }

        return isValid;
    }

    /**
     * Clears all error maps in preparation for a new validation cycle.
     */
    private void clearErrors() {
        allErrors.clear();
        firstErrors.clear();
    }

    /**
     * Logs the results of form verification.
     * 
     * @param isValid Whether the verification passed successfully
     */
    private void logVerificationResults(boolean isValid) {
        String logMessage = "Form verification " + buildVerificationLog();
        logger.info(() -> logMessage);

        if (!isValid) {
            logger.info(() -> "Form validation failed with errors: " + firstErrors.toString());
        }
    }

    /**
     * Builds a log-friendly string showing each field's status or error message.
     */
    private String buildVerificationLog() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (var entry : fieldValidators.entrySet()) {
            String fieldName = entry.getKey();
            String value = fieldValues.get(fieldName);

            if (firstErrors.containsKey(fieldName)) {
                sb.append("[").append(fieldName)
                        .append(" => ERROR: ").append(firstErrors.get(fieldName)).append("], ");
            } else {
                sb.append("[").append(fieldName)
                        .append(" => \"").append(value).append("\"], ");
            }
        }

        if (!fieldValidators.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }

        sb.append("]");
        return sb.toString();
    }

    /**
     * Returns a map of the first error for each field produced during the last
     * validation call.
     * This method is for backwards compatibility with code expecting only one error
     * per field.
     *
     * @return A map from field names to their first error message
     */
    public Map<String, String> getErrors() {
        return firstErrors;
    }

    /**
     * Returns the first error message for the given field, or {@code null} if
     * no error is present.
     *
     * @param fieldName The name of the field
     * @return The first error message, or {@code null} if no error exists
     */
    public String getError(String fieldName) {
        return firstErrors.get(fieldName);
    }

    /**
     * Returns a map containing all error messages for all fields that failed
     * validation.
     * Each field can have multiple errors if multiple validation rules failed.
     *
     * @return A map from field names to lists of error messages
     */
    public Map<String, List<String>> getAllErrors() {
        Map<String, List<String>> formattedErrors = new LinkedHashMap<>();

        for (var entry : allErrors.entrySet()) {
            List<String> formatted = new ArrayList<>();

            for (String message : entry.getValue()) {
                String formattedMessage = errorTemplate
                        .replace("{field}", entry.getKey())
                        .replace("{message}", message);
                formatted.add(formattedMessage);
            }

            formattedErrors.put(entry.getKey(), formatted);
        }

        return formattedErrors;
    }

    /**
     * Adds an error for the given field. This method stores both the first error
     * for backward compatibility and adds to the complete list of all errors.
     *
     * @param fieldName    The name of the field that failed validation
     * @param errorMessage The error message describing why validation failed
     */
    public void addError(String fieldName, String errorMessage) {
        allErrors.computeIfAbsent(fieldName, k -> new ArrayList<>()).add(errorMessage);

        if (!firstErrors.containsKey(fieldName)) {
            firstErrors.put(fieldName, errorMessage);
        }
    }

    /**
     * Retrieves the raw (string) value for the given field name.
     *
     * @param fieldName The field name
     * @return The string value, or {@code null} if not set
     */
    public String getValue(String fieldName) {
        return fieldValues.get(fieldName);
    }

    /**
     * Retrieves and parses the value of a field as an {@link Integer}.
     *
     * @param fieldName The field name
     * @return The parsed integer
     * @throws FormConversionException If the value is missing or not parseable as
     *                                 an int
     */
    public Integer getValueAsInt(String fieldName) {
        String value = getValue(fieldName);
        validateFieldValueExists(fieldName, value, "int");

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw createConversionException(fieldName, value, "int", e);
        }
    }

    /**
     * Retrieves and parses the value of a field as a {@link Double}.
     *
     * @param fieldName The field name
     * @return The parsed double
     * @throws FormConversionException If the value is missing or not parseable as a
     *                                 double
     */
    public Double getValueAsDouble(String fieldName) {
        String value = getValue(fieldName);
        validateFieldValueExists(fieldName, value, "double");

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw createConversionException(fieldName, value, "double", e);
        }
    }

    /**
     * Retrieves and parses the value of a field as a {@link Boolean}.
     *
     * @param fieldName The field name
     * @return The parsed boolean
     * @throws FormConversionException If the value is missing (null or empty)
     */
    public Boolean getValueAsBoolean(String fieldName) {
        String value = getValue(fieldName);
        validateFieldValueExists(fieldName, value, "boolean");

        return Boolean.parseBoolean(value);
    }

    /**
     * Retrieves and parses the value of a field as a {@link LocalDate}, using
     * either a custom date pattern (if registered) or {@code "yyyy-MM-dd"} by
     * default.
     *
     * @param fieldName The field name
     * @return The parsed date
     * @throws FormConversionException If the value is missing or cannot be parsed
     */
    public LocalDate getValueAsDate(String fieldName) {
        String value = getValue(fieldName);
        validateFieldValueExists(fieldName, value, "date");

        String pattern = datePatterns.getOrDefault(fieldName, DEFAULT_DATE_PATTERN);
        return parseDate(fieldName, value, pattern);
    }

    /**
     * Retrieves and parses the value of a field as a {@link LocalDate},
     * using the specified date pattern rather than any pattern in
     * {@link #datePatterns}.
     *
     * @param fieldName The field name
     * @param pattern   The date pattern (e.g., "MM/dd/yyyy")
     * @return The parsed date
     * @throws FormConversionException If the value is missing or cannot be parsed
     */
    public LocalDate getValueAsDate(String fieldName, String pattern) {
        String value = getValue(fieldName);
        validateFieldValueExists(fieldName, value, "date");

        return parseDate(fieldName, value, pattern);
    }

    /**
     * Validates that a field value exists and is not empty.
     * 
     * @param fieldName  The name of the field being validated
     * @param value      The value to check
     * @param targetType The type being converted to (for error messaging)
     * @throws FormConversionException If the value is null or empty
     */
    private void validateFieldValueExists(String fieldName, String value, String targetType) {
        if (value == null || value.isEmpty()) {
            throw new FormConversionException(
                    "The conversion of field " + fieldName + " to " + targetType + " failed. The field is empty.");
        }
    }

    /**
     * Creates a FormConversionException for a failed conversion.
     * 
     * @param fieldName  The name of the field
     * @param value      The value that failed conversion
     * @param targetType The type being converted to
     * @param cause      The underlying exception
     * @return A FormConversionException with appropriate message
     */
    private FormConversionException createConversionException(String fieldName, String value, String targetType,
            Exception cause) {
        return new FormConversionException(
                "The conversion of field " + fieldName + " to " + targetType + " failed. The field is " + value + ".",
                cause);
    }

    /**
     * Parses a string value to a LocalDate using the specified pattern.
     * 
     * @param fieldName The name of the field
     * @param value     The string value to parse
     * @param pattern   The date pattern to use
     * @return The parsed LocalDate
     * @throws FormConversionException If parsing fails
     */
    private LocalDate parseDate(String fieldName, String value, String pattern) {
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException e) {
            throw createConversionException(fieldName, value, "date", e);
        }
    }

    /**
     * Registers a custom date format pattern for the specified field,
     * overriding the default "yyyy-MM-dd" used by {@link #getValueAsDate(String)}.
     *
     * @param fieldName The field name that will use the pattern
     * @param pattern   The date format pattern (e.g., "yyyy/MM/dd")
     */
    void registerDatePattern(String fieldName, String pattern) {
        datePatterns.put(fieldName, pattern);
    }

    /**
     * Creates a copy of the form's field values, excluding specified fields.
     * 
     * @param ignoreFields Field names to exclude
     * @return A map containing all field values except those ignored
     */
    private Map<String, String> getFilteredFieldValues(String... ignoreFields) {
        Map<String, String> data = new HashMap<>(fieldValues);
        for (String ignore : ignoreFields) {
            data.remove(ignore);
        }
        return data;
    }

    /**
     * Attempts to build an instance of the specified class from the form data.
     * It converts the form's field values map into a JSON string and then
     * deserializes that JSON into the target entity.
     * <p>
     * Fields specified in {@code ignoreFields} are omitted from the conversion.
     *
     * @param <T>          The type of the class
     * @param clazz        The class to instantiate
     * @param ignoreFields Field names to exclude from conversion
     * @return An instance of T populated with the form's field values
     * @throws FormConversionException If conversion fails
     */
    public <T> T buildEntity(Class<T> clazz, String... ignoreFields) {
        try {
            Map<String, String> data = getFilteredFieldValues(ignoreFields);
            String json = JacksonUtil.getObjectMapper().writeValueAsString(data);
            return JacksonUtil.getObjectMapper().readValue(json, clazz);
        } catch (Exception e) {
            throw new FormConversionException("Failed to build entity from form", e);
        }
    }

    /**
     * Attempts to build a JoltModel from the form data, excluding specified fields.
     * 
     * This method creates a new JoltModel containing all field values from the
     * form, except for those specified in the ignoreFields parameter.
     * 
     * @param ignoreFields Field names to exclude from the model
     * @return A JoltModel populated with the form's field values (minus ignored
     *         fields)
     */
    public JoltModel buildModel(String... ignoreFields) {
        Map<String, Object> data = new HashMap<>(fieldValues);
        for (String ignore : ignoreFields) {
            data.remove(ignore);
        }
        return JoltModel.from(data);
    }

    /**
     * Updates the provided entity instance with values from the form's field
     * values, ignoring any fields specified in the {@code ignoreFields} array.
     * <p>
     * This method creates a copy of the form's field values, removes any entries
     * whose keys are present in the {@code ignoreFields}, and then uses Jackson's
     * {@link ObjectMapper#updateValue(Object, Object)}
     * to update the entity with the remaining values. Any form fields that do not
     * correspond to properties in the entity are ignored.
     *
     * @param <T>          The type of the entity.
     * @param entity       The existing entity instance to be updated.
     * @param ignoreFields Field names to exclude from updating.
     * @return The updated entity instance.
     * @throws FormConversionException If an error occurs during the update process.
     */
    public <T> T updateEntity(T entity, String... ignoreFields) {
        try {
            Map<String, String> data = getFilteredFieldValues(ignoreFields);
            JacksonUtil.getObjectMapper().updateValue(entity, data);
            return entity;
        } catch (Exception e) {
            throw new FormConversionException("Failed to update entity from form", e);
        }
    }

    /**
     * Changes how errors are displayed in {@link #getAllErrors()}, by
     * allowing a custom template string with <code>{field}</code> and
     * <code>{message}</code> placeholders.
     *
     * @param template The template for formatting error strings
     * @return This {@code Form} (for fluent chaining)
     */
    public Form setErrorTemplate(String template) {
        this.errorTemplate = template;
        return this;
    }

    /**
     * Specifies a callback to run if the form validation succeeds.
     *
     * @param callback A {@link Runnable} to execute upon successful validation
     * @return This {@code Form} (for fluent chaining)
     */
    public Form onSuccess(Runnable callback) {
        this.successCallback = callback;
        return this;
    }

    /**
     * Creates a quick, single-field {@code Form} and returns a
     * {@link FieldValidator} for it, useful for one-off validations.
     *
     * @param fieldName The name of the field being validated
     * @param value     The initial value for that field
     * @return A new {@link FieldValidator} for chaining additional rules
     */
    public static FieldValidator validateField(String fieldName, String value) {
        Form form = new Form();
        form.setValue(fieldName, value);
        return form.field(fieldName);
    }
}