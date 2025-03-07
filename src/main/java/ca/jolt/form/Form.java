package ca.jolt.form;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.jolt.exceptions.FormConversionException;

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
 * <li>Perform synchronous validation via {@link #verify()}.</li>
 * <li>Perform asynchronous validation (supporting {@link AsyncRule}) via
 * {@link #verifyAsync()}.</li>
 * <li>Track validation errors in a dedicated map.</li>
 * </ul>
 * <p>
 * <strong>Usage Example:</strong>
 * 
 * <pre>{@code
 *
 * Form form = new Form(); // You can also do: ctx.queryToForm() or ctx.bodyToForm()
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
public final class Form {

    private static final Logger logger = Logger.getLogger(Form.class.getName());

    /**
     * Maps each field name to its current string value.
     */
    private final Map<String, String> fieldValues = new LinkedHashMap<>();

    /**
     * Maps each field name to its associated {@link FieldValidator}.
     */
    private final Map<String, FieldValidator> fieldValidators = new LinkedHashMap<>();

    /**
     * Stores error messages for fields that fail validation.
     */
    private final Map<String, String> errors = new LinkedHashMap<>();

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
            for (var entry : initialData.entrySet()) {
                fieldValues.put(entry.getKey(), entry.getValue());
            }
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
        errors.clear();
        boolean isValid = true;
        for (FieldValidator validator : fieldValidators.values()) {
            if (!validator.verify()) {
                isValid = false;
            }
        }

        verifyLog(isValid);

        if (isValid && successCallback != null) {
            successCallback.run();
        }

        return isValid;
    }

    private void verifyLog(boolean isValid) {
        String logMessage = "Form verification " + buildVerificationLog();
        logger.info(() -> logMessage);
        if (!isValid) {
            logger.info(() -> "Form validation failed with errors: " + errors.toString());
        }
    }

    /**
     * Builds a log-friendly string showing each field’s status or error message.
     */
    private String buildVerificationLog() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (var entry : fieldValidators.entrySet()) {
            String fieldName = entry.getKey();
            String value = fieldValues.get(fieldName);
            if (errors.containsKey(fieldName)) {
                sb.append("[").append(fieldName)
                        .append(" => ERROR: ").append(errors.get(fieldName)).append("], ");
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
     * Runs asynchronous validation on all fields (except those excluded)
     * to support any {@link AsyncRule} objects.
     * <p>
     * This method:
     * <ul>
     * <li>Clears any existing errors.</li>
     * <li>Collects asynchronous validations only for fields not listed
     * in {@code excludedFields}.</li>
     * <li>Runs synchronous validation on non‑excluded fields.</li>
     * <li>Logs the process and returns a CompletableFuture that resolves
     * to true only if both sync and async validations pass.</li>
     * </ul>
     *
     * @param excludedFields Field names to skip during validation
     * @return A CompletableFuture that resolves to true if all validations pass;
     *         false otherwise
     */
    public CompletableFuture<Boolean> verifyAsync(String... excludedFields) {
        errors.clear();
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        List<String> excluded = Arrays.asList(excludedFields);

        for (var entry : fieldValidators.entrySet()) {
            String fieldName = entry.getKey();
            if (excluded.contains(fieldName)) {
                continue;
            }
            FieldValidator validator = entry.getValue();
            String value = fieldValues.getOrDefault(fieldName, "");
            addAsyncValidationFutures(futures, fieldName, validator, value);
        }

        boolean syncValid = verify(excludedFields);
        verifyLog(syncValid);

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    boolean allValid = true;
                    for (CompletableFuture<Boolean> future : futures) {
                        try {
                            if (!future.join()) {
                                allValid = false;
                            }
                        } catch (Exception e) {
                            allValid = false;
                            logger.warning("Async validation exception: " + e.getMessage());
                        }
                    }
                    boolean finalResult = syncValid && allValid;
                    return finalResult;
                });
    }

    /**
     * Helper method that gathers all {@link AsyncRule} instances from a validator
     * and schedules them.
     */
    private void addAsyncValidationFutures(List<CompletableFuture<Boolean>> futures,
            String fieldName,
            FieldValidator validator,
            String value) {
        for (Rule rule : validator.getRules()) {
            if (rule instanceof AsyncRule asyncRule) {
                futures.add(asyncRule.validateAsync(value)
                        .thenApply(valid -> {
                            if (!valid) {
                                addError(fieldName, rule.getErrorMessage());
                            }
                            return valid;
                        }));
            }
        }
    }

    /**
     * Returns a map of all errors produced during the last validation call
     * ({@link #verify()} or {@link #verifyAsync()}).
     *
     * @return A map from field names to error messages
     */
    public Map<String, String> getErrors() {
        return errors;
    }

    /**
     * Returns the error message for the given field, or {@code null} if
     * no error is present.
     *
     * @param fieldName The name of the field
     * @return The error message, or {@code null} if no error exists
     */
    public String getError(String fieldName) {
        return errors.get(fieldName);
    }

    /**
     * Returns all errors formatted using {@link #errorTemplate}.
     * <p>
     * Each error is a string where <code>{field}</code> is replaced by the
     * field name and <code>{message}</code> is replaced by the associated
     * error message.
     *
     * @return A list of formatted error strings
     */
    public List<String> getAllErrors() {
        List<String> allErrors = new ArrayList<>();
        for (var entry : errors.entrySet()) {
            String formatted = errorTemplate
                    .replace("{field}", entry.getKey())
                    .replace("{message}", entry.getValue());
            allErrors.add(formatted);
        }
        return allErrors;
    }

    /**
     * Manually adds an error for the given field.
     *
     * @param fieldName    The name of the field that failed validation
     * @param errorMessage The error message describing why validation failed
     */
    public void addError(String fieldName, String errorMessage) {
        errors.put(fieldName, errorMessage);
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
        if (value == null || value.isEmpty()) {
            throw new FormConversionException(
                    "The conversion of field " + fieldName + " to int failed. The field is empty.");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new FormConversionException(
                    "The conversion of field " + fieldName + " to int failed. The field is " + value + ".", e);
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
        if (value == null || value.isEmpty()) {
            throw new FormConversionException(
                    "The conversion of field " + fieldName + " to double failed. The field is empty.");
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new FormConversionException(
                    "The conversion of field " + fieldName + " to date failed. The field is " + value + ".", e);
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
        if (value == null || value.isEmpty()) {
            throw new FormConversionException(
                    "The conversion of field " + fieldName + " to boolean failed. The field is empty.");
        }
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
        if (value == null || value.isEmpty()) {
            throw new FormConversionException(
                    "The conversion of field " + fieldName + " to date failed. The field is empty.");
        }
        String pattern = datePatterns.getOrDefault(fieldName, "yyyy-MM-dd");
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException e) {
            throw new FormConversionException(
                    "The conversion of field " + fieldName + " to date failed. The field is " + value + ".", e);
        }
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
        if (value == null || value.isEmpty()) {
            throw new FormConversionException(
                    "The conversion of field " + fieldName + " to date failed. The field is empty.");
        }
        try {
            return LocalDate.parse(value, DateTimeFormatter.ofPattern(pattern));
        } catch (DateTimeParseException e) {
            throw new FormConversionException(
                    "The conversion of field " + fieldName + " to date failed. The field is " + value + ".", e);
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
        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, String> data = new HashMap<>(fieldValues);
            for (String ignore : ignoreFields) {
                data.remove(ignore);
            }
            String json = mapper.writeValueAsString(data);
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new FormConversionException("Failed to build entity from form", e);
        }
    }

    /**
     * Convenience method to build an entity without excluding any fields.
     *
     * @param <T>   The type of the class
     * @param clazz The class to instantiate
     * @return An instance of T populated with the form's field values
     * @throws FormConversionException If conversion fails
     */
    public <T> T buildEntity(Class<T> clazz) {
        return buildEntity(clazz, new String[0]);
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
