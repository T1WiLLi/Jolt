package io.github.t1willi.form;

import java.time.LocalDate;
import java.util.function.Predicate;

/**
 * A fluent API for validating and converting a single form field's data.
 * <p>
 * The {@code Field} interface provides methods to define validation rules and
 * convert the value of a
 * single form field in a web application. It supports a fluent, chainable API
 * for specifying
 * constraints such as required fields, minimum/maximum lengths, data formats
 * (e.g., email, phone
 * number), and custom rules. Additionally, it offers methods to retrieve the
 * field's value as
 * different data types (e.g., String, Integer, LocalDate). This interface is
 * typically used in
 * conjunction with the {@link Form} interface to process and validate form
 * submissions in a web
 * application. Default methods provide convenience overloads with predefined
 * error messages to
 * simplify common validation scenarios.
 *
 * @since 1.0.0
 */
public interface Field {

    /**
     * Marks the field as required, adding an error message if the field is empty or
     * null.
     * <p>
     * This method specifies that the field must have a non-empty, non-null value.
     * If the validation
     * fails, the provided error message is associated with the field. This method
     * supports method
     * chaining to allow multiple validation rules to be applied fluently.
     *
     * @param msg the error message to use if the field is empty or null
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the error message is null
     * @since 1.0.0
     */
    Field required(String msg);

    /**
     * Specifies a minimum length for the field's value, adding an error message if
     * the length is too short.
     * <p>
     * This method validates that the field's value has at least the specified
     * number of characters.
     * If the validation fails, the provided error message is associated with the
     * field. This is
     * typically used for string-based fields like text inputs.
     *
     * @param min the minimum length of the field's value
     * @param msg the error message to use if the length is less than the minimum
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the minimum length is negative or the
     *                                  error message is null
     * @since 1.0.0
     */
    Field min(long min, String msg);

    /**
     * Specifies a maximum length for the field's value, adding an error message if
     * the length is too long.
     * <p>
     * This method validates that the field's value does not exceed the specified
     * number of characters.
     * If the validation fails, the provided error message is associated with the
     * field. This is
     * typically used for string-based fields like text inputs.
     *
     * @param max the maximum length of the field's value
     * @param msg the error message to use if the length exceeds the maximum
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the maximum length is negative or the
     *                                  error message is null
     * @since 1.0.0
     */
    Field max(long max, String msg);

    /**
     * Validates that the field's value is a valid email address, adding an error
     * message if invalid.
     * <p>
     * This method checks if the field's value conforms to a standard email address
     * format (e.g.,
     * user@domain.com). If the validation fails, the provided error message is
     * associated with the field.
     *
     * @param msg the error message to use if the email format is invalid
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the error message is null
     * @since 1.0.0
     */
    Field email(String msg);

    /**
     * Validates that the field's value contains only alphanumeric characters,
     * adding an error message if invalid.
     * <p>
     * This method checks if the field's value consists solely of letters (a-z, A-Z)
     * and digits (0-9).
     * If the validation fails, the provided error message is associated with the
     * field.
     *
     * @param msg the error message to use if non-alphanumeric characters are found
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the error message is null
     * @since 1.0.0
     */
    Field alphanumeric(String msg);

    /**
     * Validates that the field's value is a valid phone number, adding an error
     * message if invalid.
     * <p>
     * This method checks if the field's value conforms to a standard phone number
     * format (e.g.,
     * including digits, hyphens, or parentheses). The exact format depends on the
     * implementation.
     * If the validation fails, the provided error message is associated with the
     * field.
     *
     * @param msg the error message to use if the phone number format is invalid
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the error message is null
     * @since 1.0.0
     */
    Field phoneNumber(String msg);

    /**
     * Validates that the field's value is a valid ZIP code, adding an error message
     * if invalid.
     * <p>
     * This method checks if the field's value conforms to a standard ZIP code
     * format (e.g., 5-digit or
     * 5+4 format for US ZIP codes). The exact format depends on the implementation.
     * If the validation
     * fails, the provided error message is associated with the field.
     *
     * @param msg the error message to use if the ZIP code format is invalid
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the error message is null
     * @since 1.0.0
     */
    Field zipCode(String msg);

    /**
     * Validates that the field's value is a valid URL, adding an error message if
     * invalid.
     * <p>
     * This method checks if the field's value conforms to a standard URL format
     * (e.g.,
     * http://example.com). If the validation fails, the provided error message is
     * associated with the
     * field.
     *
     * @param msg the error message to use if the URL format is invalid
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the error message is null
     * @since 1.0.0
     */
    Field url(String msg);

    /**
     * Validates that the field's value is a valid date, adding an error message if
     * invalid.
     * <p>
     * This method checks if the field's value can be parsed as a valid date, using
     * a default date
     * format defined by the implementation. If the validation fails, the provided
     * error message is
     * associated with the field.
     *
     * @param msg the error message to use if the date format is invalid
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the error message is null
     * @since 1.0.0
     */
    Field date(String msg);

    /**
     * Validates that the field's value is a valid date with a specific format,
     * adding an error message if invalid.
     * <p>
     * This method checks if the field's value can be parsed as a valid date
     * according to the specified
     * pattern (e.g., "yyyy-MM-dd"). If the validation fails, the provided error
     * message is associated
     * with the field.
     *
     * @param pat the date format pattern (e.g., "yyyy-MM-dd")
     * @param msg the error message to use if the date format is invalid
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the pattern or error message is null
     * @since 1.0.0
     */
    Field date(String pat, String msg);

    /**
     * Validates that the field's value is a valid credit card number, adding an
     * error message if invalid.
     * <p>
     * This method checks if the field's value conforms to a standard credit card
     * number format (e.g.,
     * passing Luhn algorithm validation). If the validation fails, the provided
     * error message is
     * associated with the field.
     *
     * @param msg the error message to use if the credit card number is invalid
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the error message is null
     * @since 1.0.0
     */
    Field creditCard(String msg);

    /**
     * Validates that the field's value is a strong password, adding an error
     * message if invalid.
     * <p>
     * This method checks if the field's value meets strong password criteria (e.g.,
     * minimum length,
     * mix of character types). The exact criteria depend on the implementation. If
     * the validation
     * fails, the provided error message is associated with the field.
     *
     * @param msg the error message to use if the password is too weak
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the error message is null
     * @since 1.0.0
     */
    Field strongPassword(String msg);

    /**
     * Validates that the field's value is a valid IP address, adding an error
     * message if invalid.
     * <p>
     * This method checks if the field's value conforms to a standard IPv4 or IPv6
     * address format.
     * If the validation fails, the provided error message is associated with the
     * field.
     *
     * @param msg the error message to use if the IP address format is invalid
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the error message is null
     * @since 1.0.0
     */
    Field ipAddress(String msg);

    /**
     * Validates that the field's numeric value is less than the threshold, adding
     * an error message if invalid.
     * <p>
     * This method checks if the field's value, when parsed as a number, is less
     * than the specified
     * threshold. If the validation fails, the provided error message is associated
     * with the field.
     *
     * @param thr the threshold value to compare against
     * @param msg the error message to use if the value is not less than the
     *            threshold
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the threshold or error message is null
     * @throws NumberFormatException    if the field's value cannot be parsed as a
     *                                  number
     * @since 1.0.0
     */
    Field lowerThan(Number thr, String msg);

    /**
     * Validates that the field's numeric value is less than or equal to the
     * threshold, adding an error message if invalid.
     * <p>
     * This method checks if the field's value, when parsed as a number, is less
     * than or equal to the
     * specified threshold. If the validation fails, the provided error message is
     * associated with the
     * field.
     *
     * @param thr the threshold value to compare against
     * @param msg the error message to use if the value is not less than or equal to
     *            the threshold
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the threshold or error message is null
     * @throws NumberFormatException    if the field's value cannot be parsed as a
     *                                  number
     * @since 1.0.0
     */
    Field lowerEqualsThan(Number thr, String msg);

    /**
     * Validates that the field's numeric value is greater than the threshold,
     * adding an error message if invalid.
     * <p>
     * This method checks if the field's value, when parsed as a number, is greater
     * than the specified
     * threshold. If the validation fails, the provided error message is associated
     * with the field.
     *
     * @param thr the threshold value to compare against
     * @param msg the error message to use if the value is not greater than the
     *            threshold
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the threshold or error message is null
     * @throws NumberFormatException    if the field's value cannot be parsed as a
     *                                  number
     * @since 1.0.0
     */
    Field greaterThan(Number thr, String msg);

    /**
     * Validates that the field's numeric value is greater than or equal to the
     * threshold, adding an error message if invalid.
     * <p>
     * This method checks if the field's value, when parsed as a number, is greater
     * than or equal to
     * the specified threshold. If the validation fails, the provided error message
     * is associated with
     * the field.
     *
     * @param thr the threshold value to compare against
     * @param msg the error message to use if the value is notों
     * 
     *            System: greater than or equal to the threshold
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the threshold or error message is null
     * @throws NumberFormatException    if the field's value cannot be parsed as a
     *                                  number
     * @since 1.0.0
     */
    Field greaterEqualsThan(Number thr, String msg);

    /**
     * Validates that the field's numeric value is within the specified range,
     * adding an error message if invalid.
     * <p>
     * This method checks if the field's value, when parsed as a number, is between
     * the specified minimum
     * and maximum values (inclusive). If the validation fails, the provided error
     * message is associated
     * with the field.
     *
     * @param min the minimum value (inclusive) for the field's value
     * @param max the maximum value (inclusive) for the field's value
     * @param msg the error message to use if the value is outside the range
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the minimum, maximum, or error message is
     *                                  null, or if min is greater than max
     * @throws NumberFormatException    if the field's value cannot be parsed as a
     *                                  number
     * @since 1.0.0
     */
    Field clamp(Number min, Number max, String msg);

    /**
     * Validates that the field's value matches the specified regular expression,
     * adding an error message if invalid.
     * <p>
     * This method checks if the field's value conforms to the provided regular
     * expression pattern.
     * If the validation fails, the provided error message is associated with the
     * field.
     *
     * @param pat the regular expression pattern to match against
     * @param msg the error message to use if the value does not match the pattern
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException               if the pattern or error
     *                                                message is null
     * @throws java.util.regex.PatternSyntaxException if the regular expression
     *                                                pattern is invalid
     * @since 1.0.0
     */
    Field regex(String pat, String msg);

    /**
     * Validates the field's value using a custom predicate, adding an error message
     * if invalid.
     * <p>
     * This method applies the provided predicate to the field's value. If the
     * predicate returns
     * {@code false}, the provided error message is associated with the field. This
     * allows for
     * custom validation logic beyond the standard rules provided by this interface.
     *
     * @param pred the predicate to test the field's value
     * @param msg  the error message to use if the predicate returns {@code false}
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the predicate or error message is null
     * @since 1.0.0
     */
    Field rule(Predicate<String> pred, String msg);

    /**
     * Marks the field as required with a default error message.
     * <p>
     * This method is a convenience overload for {@link #required(String)} that uses
     * a default error
     * message ("Value is required."). It marks the field as required, ensuring it
     * is non-empty and
     * non-null.
     *
     * @return this {@link Field} instance for method chaining
     * @since 1.0.0
     */
    default Field required() {
        return required("Value is required.");
    }

    /**
     * Specifies a minimum length for the field's value with a default error
     * message.
     * <p>
     * This method is a convenience overload for {@link #min(long, String)} that
     * uses a default error
     * message ("Minimum length is " + min + "."). It validates that the field's
     * value has at least the
     * specified number of characters.
     *
     * @param m the minimum length of the field's value
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the minimum length is negative
     * @since 1.0.0
     */
    default Field min(long m) {
        return min(m, "Minimum length is " + m + ".");
    }

    /**
     * Specifies a maximum length for the field's value with a default error
     * message.
     * <p>
     * This method is a convenience overload for {@link #max(long, String)} that
     * uses a default error
     * message ("Maximum length is " + max + "."). It validates that the field's
     * value does not exceed
     * the specified number of characters.
     *
     * @param m the maximum length of the field's value
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the maximum length is negative
     * @since 1.0.0
     */
    default Field max(long m) {
        return max(m, "Maximum length is " + m + ".");
    }

    /**
     * Validates that the field's value is a valid email address with a default
     * error message.
     * <p>
     * This method is a convenience overload for {@link #email(String)} that uses a
     * default error
     * message ("Invalid email."). It checks if the field's value conforms to a
     * standard email address
     * format.
     *
     * @return this {@link Field} instance for method chaining
     * @since 1.0.0
     */
    default Field email() {
        return email("Invalid email.");
    }

    /**
     * Validates that the field's value contains only alphanumeric characters with a
     * default error message.
     * <p>
     * This method is a convenience overload for {@link #alphanumeric(String)} that
     * uses a default error
     * message ("Only letters and digits allowed."). It checks if the field's value
     * consists solely of
     * letters and digits.
     *
     * @return this {@link Field} instance for method chaining
     * @since 1.0.0
     */
    default Field alphanumeric() {
        return alphanumeric("Only letters and digits allowed.");
    }

    /**
     * Validates that the field's value is a valid phone number with a default error
     * message.
     * <p>
     * This method is a convenience overload for {@link #phoneNumber(String)} that
     * uses a default error
     * message ("Invalid phone number."). It checks if the field's value conforms to
     * a standard phone
     * number format.
     *
     * @return this {@link Field} instance for method chaining
     * @since 1.0.0
     */
    default Field phoneNumber() {
        return phoneNumber("Invalid phone number.");
    }

    /**
     * Validates that the field's value is a valid ZIP code with a default error
     * message.
     * <p>
     * This method is a convenience overload for {@link #zipCode(String)} that uses
     * a default error
     * message ("Invalid ZIP code."). It checks if the field's value conforms to a
     * standard ZIP code
     * format.
     *
     * @return this {@link Field} instance for method chaining
     * @since 1.0.0
     */
    default Field zipCode() {
        return zipCode("Invalid ZIP code.");
    }

    /**
     * Validates that the field's value is a valid URL with a default error message.
     * <p>
     * This method is a convenience overload for {@link #url(String)} that uses a
     * default error message
     * ("Invalid URL."). It checks if the field's value conforms to a standard URL
     * format.
     *
     * @return this {@link Field} instance for method chaining
     * @since 1.0.0
     */
    default Field url() {
        return url("Invalid URL.");
    }

    /**
     * Validates that the field's value is a valid date with a default error
     * message.
     * <p>
     * This method is a convenience overload for {@link #date(String)} that uses a
     * default error
     * message ("Invalid date."). It checks if the field's value can be parsed as a
     * valid date using
     * the implementation's default format.
     *
     * @return this {@link Field} instance for method chaining
     * @since 1.0.0
     */
    default Field date() {
        return date("Invalid date.");
    }

    /**
     * Validates that the field's value is a valid credit card number with a default
     * error message.
     * <p>
     * This method is a convenience overload for {@link #creditCard(String)} that
     * uses a default error
     * message ("Invalid credit card."). It checks if the field's value conforms to
     * a standard credit
     * card number format.
     *
     * @return this {@link Field} instance for method chaining
     * @since 1.0.0
     */
    default Field creditCard() {
        return creditCard("Invalid credit card.");
    }

    /**
     * Validates that the field's value is a strong password with a default error
     * message.
     * <p>
     * This method is a convenience overload for {@link #strongPassword(String)}
     * that uses a default
     * error message ("Password too weak."). It checks if the field's value meets
     * strong password
     * criteria.
     *
     * @return this {@link Field} instance for method chaining
     * @since 1.0.0
     */
    default Field strongPassword() {
        return strongPassword("Password too weak.");
    }

    /**
     * Validates that the field's value is a valid IP address with a default error
     * message.
     * <p>
     * This method is a convenience overload for {@link #ipAddress(String)} that
     * uses a default error
     * message ("Invalid IP address."). It checks if the field's value conforms to a
     * standard IP address
     * format.
     *
     * @return this {@link Field} instance for method chaining
     * @since 1.0.0
     */
    default Field ipAddress() {
        return ipAddress("Invalid IP address.");
    }

    /**
     * Validates that the field's numeric value is within the specified range with a
     * default error message.
     * <p>
     * This method is a convenience overload for
     * {@link #clamp(Number, Number, String)} that uses a
     * default error message ("Value must be between " + a + " and " + b + "."). It
     * checks if the
     * field's value is between the specified minimum and maximum values
     * (inclusive).
     *
     * @param a the minimum value (inclusive) for the field's value
     * @param b the maximum value (inclusive) for the field's value
     * @return this {@link Field} instance for method chaining
     * @throws IllegalArgumentException if the minimum or maximum is null, or if min
     *                                  is greater than max
     * @throws NumberFormatException    if the field's value cannot be parsed as a
     *                                  number
     * @since 1.0.0
     */
    default Field clamp(Number a, Number b) {
        return clamp(a, b, "Value must be between " + a + " and " + b + ".");
    }

    /**
     * Retrieves the field's value as a String.
     * <p>
     * This method returns the raw string value of the field. If the field is empty
     * or null, it may
     * return null or an empty string, depending on the implementation.
     *
     * @return the field's value as a String, or null if the field is unset
     * @since 1.0.0
     */
    String get();

    /**
     * Converts the field's value to an Integer.
     * <p>
     * This method attempts to parse the field's value as an integer. If the value
     * cannot be parsed,
     * an exception is thrown.
     *
     * @return the field's value as an Integer
     * @throws NumberFormatException if the field's value cannot be parsed as an
     *                               integer
     * @since 1.0.0
     */
    Integer asInt();

    /**
     * Converts the field's value to a Double.
     * <p>
     * This method attempts to parse the field's value as a double. If the value
     * cannot be parsed,
     * an exception is thrown.
     *
     * @return the field's value as a Double
     * @throws NumberFormatException if the field's value cannot be parsed as a
     *                               double
     * @since 1.0.0
     */
    Double asDouble();

    /**
     * Converts the field's value to a Boolean.
     * <p>
     * This method attempts to parse the field's value as a boolean (e.g., "true" or
     * "false"). If the
     * value cannot be parsed, an exception is thrown.
     *
     * @return the field's value as a Boolean
     * @throws IllegalArgumentException if the field's value cannot be parsed as a
     *                                  boolean
     * @since 1.0.0
     */
    Boolean asBoolean();

    /**
     * Converts the field's value to a LocalDate using the default date format.
     * <p>
     * This method attempts to parse the field's value as a date using the
     * implementation's default
     * date format. If the value cannot be parsed, an exception is thrown.
     *
     * @return the field's value as a LocalDate
     * @throws java.time.format.DateTimeParseException if the field's value cannot
     *                                                 be parsed as a date
     * @since 1.0.0
     */
    LocalDate asDate();

    /**
     * Converts the field's value to a LocalDate using the specified date format.
     * <p>
     * This method attempts to parse the field's value as a date using the provided
     * pattern (e.g.,
     * "yyyy-MM-dd"). If the value cannot be parsed, an exception is thrown.
     *
     * @param pat the date format pattern (e.g., "yyyy-MM-dd")
     * @return the field's value as a LocalDate
     * @throws IllegalArgumentException                if the pattern is null
     * @throws java.time.format.DateTimeParseException if the field's value cannot
     *                                                 be parsed as a date
     * @since 1.0.0
     */
    LocalDate asDate(String pat);
}