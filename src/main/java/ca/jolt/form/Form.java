package ca.jolt.form;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import ca.jolt.exceptions.FormConversionException;

public final class Form {
    private static final Logger logger = Logger.getLogger(Form.class.getName());

    private final Map<String, String> fieldValues = new LinkedHashMap<>();
    private final Map<String, FieldValidator> fieldValidators = new LinkedHashMap<>();
    private final Map<String, String> errors = new LinkedHashMap<>();
    private final Map<String, String> datePatterns = new HashMap<>();
    private String errorTemplate = "{field}: {message}";
    private Runnable successCallback;

    public Form(Map<String, String> initialData) {
        if (initialData != null) {
            for (var entry : initialData.entrySet()) {
                fieldValues.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public Form() {
        this(null);
    }

    public FieldValidator field(String fieldName) {
        fieldValidators.computeIfAbsent(fieldName, name -> new FieldValidator(name, this));
        return fieldValidators.get(fieldName);
    }

    public Form setValue(String fieldName, String value) {
        fieldValues.put(fieldName, value);
        return this;
    }

    public boolean verify() {
        errors.clear();
        boolean isValid = true;

        for (var entry : fieldValidators.entrySet()) {
            String fieldName = entry.getKey();
            FieldValidator validator = entry.getValue();
            String value = fieldValues.getOrDefault(fieldName, "");

            if (!validator.validate(value, fieldValues)) {
                isValid = false;
            }
        }

        String logMessage = "Form verification " + buildVerificationLog();
        logger.info(() -> logMessage);

        if (!isValid) {
            logger.info(() -> "Form validation failed with errors: " + errors.toString());
        }

        if (isValid && successCallback != null) {
            successCallback.run();
        }

        return isValid;
    }

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

    public CompletableFuture<Boolean> verifyAsync() {
        errors.clear();
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for (var entry : fieldValidators.entrySet()) {
            String fieldName = entry.getKey();
            FieldValidator validator = entry.getValue();
            String value = fieldValues.getOrDefault(fieldName, "");

            addAsyncValidationFutures(futures, fieldName, validator, value);
        }

        boolean syncValid = verify();
        if (futures.isEmpty()) {
            return CompletableFuture.completedFuture(syncValid);
        }
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
                        }
                    }
                    return syncValid && allValid;
                });
    }

    private void addAsyncValidationFutures(List<CompletableFuture<Boolean>> futures, String fieldName,
            FieldValidator validator, String value) {
        for (Rule rule : validator.getRules()) {
            if (rule instanceof AsyncRule asyncRule) {
                futures.add(asyncRule.validateAsync(value, fieldValues)
                        .thenApply(valid -> {
                            if (!valid) {
                                addError(fieldName, rule.getErrorMessage());
                            }
                            return valid;
                        }));
            }
        }
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public String getError(String fieldName) {
        return errors.get(fieldName);
    }

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

    public void addError(String fieldName, String errorMessage) {
        errors.put(fieldName, errorMessage);
    }

    public String getValue(String fieldName) {
        return fieldValues.get(fieldName);
    }

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

    public Boolean getValueAsBoolean(String fieldName) {
        String value = getValue(fieldName);
        if (value == null || value.isEmpty()) {
            throw new FormConversionException(
                    "The conversion of field " + fieldName + " to boolean failed. The field is empty.");
        }
        return Boolean.parseBoolean(value);
    }

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

    void registerDatePattern(String fieldName, String pattern) {
        datePatterns.put(fieldName, pattern);
    }

    public Form setErrorTemplate(String template) {
        this.errorTemplate = template;
        return this;
    }

    public Form onSuccess(Runnable callback) {
        this.successCallback = callback;
        return this;
    }

    public static FieldValidator validateField(String fieldName, String value) {
        Form form = new Form();
        form.setValue(fieldName, value);
        return form.field(fieldName);
    }
}