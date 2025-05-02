package io.github.t1willi.form;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

class DefaultField implements Field {
    private final String name;
    private final DefaultForm form;
    private final List<Rule> rules = new ArrayList<>();

    DefaultField(String name, DefaultForm form) {
        this.name = name;
        this.form = form;
    }

    private void add(Rule r) {
        rules.add(r);
    }

    @Override
    public Field required(String msg) {
        add(BaseRules.required(msg));
        return this;
    }

    @Override
    public Field minLength(int m, String msg) {
        add(BaseRules.minLength(m, msg));
        return this;
    }

    @Override
    public Field maxLength(int m, String msg) {
        add(BaseRules.maxLength(m, msg));
        return this;
    }

    @Override
    public Field email(String msg) {
        add(BaseRules.email(msg));
        return this;
    }

    @Override
    public Field alphanumeric(String msg) {
        add(BaseRules.alphanumeric(msg));
        return this;
    }

    @Override
    public Field phoneNumber(String msg) {
        add(BaseRules.phoneNumber(msg));
        return this;
    }

    @Override
    public Field zipCode(String msg) {
        add(BaseRules.zipCode(msg));
        return this;
    }

    @Override
    public Field url(String msg) {
        add(BaseRules.url(msg));
        return this;
    }

    @Override
    public Field date(String msg) {
        add(BaseRules.date(msg));
        return this;
    }

    @Override
    public Field date(String pat, String msg) {
        add(BaseRules.date(pat, msg));
        return this;
    }

    @Override
    public Field creditCard(String msg) {
        add(BaseRules.creditCard(msg));
        return this;
    }

    @Override
    public Field strongPassword(String msg) {
        add(BaseRules.strongPassword(msg));
        return this;
    }

    @Override
    public Field ipAddress(String msg) {
        add(BaseRules.ipAddress(msg));
        return this;
    }

    @Override
    public Field lowerThan(Number t, String msg) {
        add(BaseRules.lowerThan(t, msg));
        return this;
    }

    @Override
    public Field lowerEqualsThan(Number t, String msg) {
        add(BaseRules.lowerEqualsThan(t, msg));
        return this;
    }

    @Override
    public Field greaterThan(Number t, String msg) {
        add(BaseRules.greaterThan(t, msg));
        return this;
    }

    @Override
    public Field greaterEqualsThan(Number t, String msg) {
        add(BaseRules.greaterEqualsThan(t, msg));
        return this;
    }

    @Override
    public Field clamp(Number a, Number b, String msg) {
        add(BaseRules.clamp(a, b, msg));
        return this;
    }

    @Override
    public Field regex(String p, String msg) {
        add(BaseRules.regex(p, msg, ""));
        return this;
    }

    @Override
    public Field rule(Predicate<String> pred, String msg) {
        add(Rule.custom(pred, msg));
        return this;
    }

    @Override
    public String get() {
        return form.getValues().get(name);
    }

    @Override
    public Integer asInt() {
        return Integer.parseInt(get());
    }

    @Override
    public Double asDouble() {
        return Double.parseDouble(get());
    }

    @Override
    public Boolean asBoolean() {
        return Boolean.parseBoolean(get());
    }

    @Override
    public LocalDate asDate() {
        return LocalDate.parse(get(), DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Override
    public LocalDate asDate(String pat) {
        return LocalDate.parse(get(), DateTimeFormatter.ofPattern(pat));
    }

    boolean verifyOne() {
        String value = get();
        boolean isValid = true;
        boolean hasRequiredRule = false;
        String requiredErrorMessage = null;
        List<String> otherErrors = new ArrayList<>();

        for (Rule rule : rules) {
            boolean isRequiredRule = rule.getErrorMessage().contains("required");
            if (isRequiredRule) {
                hasRequiredRule = true;
                requiredErrorMessage = rule.getErrorMessage();
            }
            if (!rule.validate(value)) {
                if (isRequiredRule) {
                    form.addError(name, rule.getErrorMessage());
                    return false;
                } else {
                    otherErrors.add(rule.getErrorMessage());
                    isValid = false;
                }
            }
        }

        if (!isValid && hasRequiredRule) {
            form.addError(name, requiredErrorMessage);
            return false;
        }

        for (String error : otherErrors) {
            form.addError(name, error);
        }

        return isValid;
    }
}
