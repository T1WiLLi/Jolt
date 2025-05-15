package io.github.t1willi.form;

import java.time.LocalDate;
import java.util.function.Predicate;

/**
 * Fluent API for a single form field's validation and conversion.
 */
public interface Field {
    Field required(String msg);

    Field min(long min, String msg);

    Field max(long max, String msg);

    Field email(String msg);

    Field alphanumeric(String msg);

    Field phoneNumber(String msg);

    Field zipCode(String msg);

    Field url(String msg);

    Field date(String msg);

    Field date(String pat, String msg);

    Field creditCard(String msg);

    Field strongPassword(String msg);

    Field ipAddress(String msg);

    Field lowerThan(Number thr, String msg);

    Field lowerEqualsThan(Number thr, String msg);

    Field greaterThan(Number thr, String msg);

    Field greaterEqualsThan(Number thr, String msg);

    Field clamp(Number min, Number max, String msg);

    Field regex(String pat, String msg);

    Field rule(Predicate<String> pred, String msg);

    default Field required() {
        return required("Value is required.");
    }

    default Field min(long m) {
        return min(m, "Minimum length is " + m + ".");
    }

    default Field max(long m) {
        return max(m, "Maximum length is " + m + ".");
    }

    default Field email() {
        return email("Invalid email.");
    }

    default Field alphanumeric() {
        return alphanumeric("Only letters and digits allowed.");
    }

    default Field phoneNumber() {
        return phoneNumber("Invalid phone number.");
    }

    default Field zipCode() {
        return zipCode("Invalid ZIP code.");
    }

    default Field url() {
        return url("Invalid URL.");
    }

    default Field date() {
        return date("Invalid date.");
    }

    default Field creditCard() {
        return creditCard("Invalid credit card.");
    }

    default Field strongPassword() {
        return strongPassword("Password too weak.");
    }

    default Field ipAddress() {
        return ipAddress("Invalid IP address.");
    }

    default Field clamp(Number a, Number b) {
        return clamp(a, b, "Value must be between " + a + " and " + b + ".");
    }

    String get();

    Integer asInt();

    Double asDouble();

    Boolean asBoolean();

    LocalDate asDate();

    LocalDate asDate(String pat);
}
