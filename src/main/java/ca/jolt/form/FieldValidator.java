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

@Getter
public final class FieldValidator {
    private final String fieldName;
    private final List<Rule> rules = new ArrayList<>();
    private final List<UnaryOperator<String>> transformers = new ArrayList<>();
    private final Form form;
    private Predicate<Map<String, String>> condition;
    private Class<?> valueType;

    public FieldValidator(String fieldName, Form form) {
        this.fieldName = fieldName;
        this.form = form;
    }

    public FieldValidator addRules(Rule... rules) {
        this.rules.addAll(Arrays.asList(rules));
        return this;
    }

    public FieldValidator addRule(Rule rule) {
        rules.add(rule);
        return this;
    }

    // Chaining methods for common rules.
    public FieldValidator required(String errorMessage) {
        addRule(BaseRules.required(errorMessage));
        return this;
    }

    public FieldValidator required() {
        return required("This field is required.");
    }

    public FieldValidator minLength(int min, String errorMessage) {
        addRule(BaseRules.minLength(min, errorMessage));
        return this;
    }

    public FieldValidator minLength(int min) {
        return minLength(min, "Value must be at least " + min + " characters long.");
    }

    public FieldValidator maxLength(int max, String errorMessage) {
        addRule(BaseRules.maxLength(max, errorMessage));
        return this;
    }

    public FieldValidator maxLength(int max) {
        return maxLength(max, "Value must be at most " + max + " characters long.");
    }

    public FieldValidator email(String errorMessage) {
        addRule(BaseRules.email(errorMessage));
        return this;
    }

    public FieldValidator email() {
        return email("Invalid email format.");
    }

    public FieldValidator alphanumeric(String errorMessage) {
        addRule(BaseRules.alphanumeric(errorMessage));
        return this;
    }

    public FieldValidator alphanumeric() {
        return alphanumeric("Value must contain only letters and numbers.");
    }

    public FieldValidator phoneNumber(String errorMessage) {
        addRule(BaseRules.phoneNumber(errorMessage));
        return this;
    }

    public FieldValidator phoneNumber() {
        return phoneNumber("Invalid phone number format.");
    }

    public FieldValidator zipCode(String errorMessage) {
        addRule(BaseRules.zipCode(errorMessage));
        return this;
    }

    public FieldValidator zipCode() {
        return zipCode("Invalid zip code format.");
    }

    public FieldValidator url(String errorMessage) {
        addRule(BaseRules.url(errorMessage));
        return this;
    }

    public FieldValidator url() {
        return url("Invalid URL format.");
    }

    public FieldValidator creditCard(String errorMessage) {
        addRule(BaseRules.creditCard(errorMessage));
        return this;
    }

    public FieldValidator creditCard() {
        return creditCard("Invalid credit card number.");
    }

    public FieldValidator strongPassword(String errorMessage) {
        addRule(BaseRules.strongPassword(errorMessage));
        return this;
    }

    public FieldValidator strongPassword() {
        return strongPassword("Password must be strong.");
    }

    public FieldValidator ipAddress(String errorMessage) {
        addRule(BaseRules.ipAddress(errorMessage));
        return this;
    }

    public FieldValidator ipAddress() {
        return ipAddress("Invalid IP address format.");
    }

    public FieldValidator date(String pattern) {
        form.registerDatePattern(fieldName, pattern);
        addRule(BaseRules.date(pattern, "Invalid date format. Use " + pattern + "."));
        return this;
    }

    public FieldValidator date(String pattern, String errorMessage) {
        form.registerDatePattern(fieldName, pattern);
        addRule(BaseRules.date(pattern, errorMessage));
        return this;
    }

    public FieldValidator when(Predicate<Map<String, String>> condition) {
        this.condition = condition;
        return this;
    }

    public FieldValidator asyncRule(Function<String, CompletableFuture<Boolean>> asyncValidator, String errorMessage) {
        addRule(new AsyncRule(asyncValidator, errorMessage));
        return this;
    }

    // Type conversion.
    public FieldValidator asInt() {
        this.valueType = Integer.class;
        return this;
    }

    public FieldValidator asDouble() {
        this.valueType = Double.class;
        return this;
    }

    public FieldValidator asBoolean() {
        this.valueType = Boolean.class;
        return this;
    }

    public FieldValidator min(int min, String errorMessage) {
        asInt();
        addRule(BaseRules.greaterEqualsThan(min, errorMessage));
        return this;
    }

    public FieldValidator min(int min) {
        return min(min, "Value must be at least " + min + ".");
    }

    public FieldValidator max(int max, String errorMessage) {
        asInt();
        addRule(BaseRules.lowerEqualsThan(max, errorMessage));
        return this;
    }

    public FieldValidator max(int max) {
        return max(max, "Value must be at most " + max + ".");
    }

    public FieldValidator transform(UnaryOperator<String> transformer) {
        this.transformers.add(transformer);
        return this;
    }

    public FieldValidator trim() {
        return transform(String::trim);
    }

    public FieldValidator toLowerCase() {
        return transform(String::toLowerCase);
    }

    public FieldValidator toUpperCase() {
        return transform(String::toUpperCase);
    }

    public boolean validate(String value, Map<String, String> allValues) {
        if (condition != null && !condition.test(allValues)) {
            return true;
        }
        String transformedValue = value;
        for (UnaryOperator<String> transformer : transformers) {
            if (transformedValue != null) {
                transformedValue = transformer.apply(transformedValue);
            }
        }
        if (transformedValue != value && transformedValue != null) {
            form.setValue(fieldName, transformedValue);
            allValues.put(fieldName, transformedValue);
        }
        for (Rule rule : rules) {
            if (!rule.validate(transformedValue, allValues)) {
                form.addError(fieldName, rule.getErrorMessage());
                return false;
            }
        }
        return true;
    }
}