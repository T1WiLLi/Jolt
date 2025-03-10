package ca.jolt.form;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Provides static factory methods that produce common {@link Rule} instances.
 * <p>
 * Offers prebuilt rules such as:
 * <ul>
 * <li>{@link #required(String)} for non-empty fields</li>
 * <li>{@link #minLength(int, String)} and {@link #maxLength(int, String)}
 * for string length constraints</li>
 * <li>{@link #email(String)} for email format validation</li>
 * <li>{@link #phoneNumber(String)} for phone number format validation</li>
 * <li>{@link #creditCard(String)} for Luhn algorithm checks</li>
 * <li>{@link #regex(String, String, String)} for custom regular
 * expressions</li>
 * <li>And more, including numeric comparisons and date parsing checks</li>
 * </ul>
 * <p>
 * This class is not intended for instantiation—call its methods directly:
 * 
 * <pre>{@code
 * Rule rule = BaseRules.required("Field is required");
 * }</pre>
 *
 * @see Rule
 * @see Form
 * @see FieldValidator
 * @since 1.0
 */
final class BaseRules {

    /**
     * Private constructor to prevent instantiation.
     */
    BaseRules() {
    }

    /**
     * Ensures the field is neither {@code null} nor empty.
     *
     * @param errorMessage The error message to use if validation fails
     * @return A {@link Rule} enforcing non-null, non-empty fields
     */
    static Rule required(String errorMessage) {
        return Rule.custom(data -> {
            if (data == null)
                return false;
            if (isNumeric(data))
                return true;
            return !data.trim().isEmpty();
        }, errorMessage);
    }

    /**
     * Enforces a minimum string length.
     *
     * @param min          The minimum length
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} enforcing the specified minimum length
     */
    static Rule minLength(int min, String errorMessage) {
        return Rule.custom(data -> data != null && data.length() >= min, errorMessage);
    }

    /**
     * Enforces a maximum string length.
     *
     * @param max          The maximum length
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} enforcing the specified maximum length
     */
    static Rule maxLength(int max, String errorMessage) {
        return Rule.custom(data -> data != null && data.length() <= max, errorMessage);
    }

    /**
     * Validates an email address format.
     *
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} checking for a valid email format
     */
    static Rule email(String errorMessage) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return regex(emailRegex, errorMessage, "");
    }

    /**
     * Ensures the field contains only alphanumeric characters.
     *
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} enforcing alphanumeric input
     */
    static Rule alphanumeric(String errorMessage) {
        return regex("^[a-zA-Z0-9]+$", errorMessage, "");
    }

    /**
     * Validates phone numbers with optional plus signs, spaces,
     * dashes, and parentheses (7-20 digits).
     *
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} enforcing the specified phone format
     */
    static Rule phoneNumber(String errorMessage) {
        return regex("^\\+?[0-9\\s-()]{7,20}$", errorMessage, "");
    }

    /**
     * Validates US ZIP code format (5-digit or 5+4-digit).
     *
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} enforcing the ZIP code format
     */
    static Rule zipCode(String errorMessage) {
        return regex("^[0-9]{5}(?:-[0-9]{4})?$", errorMessage, "");
    }

    /**
     * Validates simple URL formats (supports http, https, ftp).
     *
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} enforcing basic URL format validation
     */
    static Rule url(String errorMessage) {
        String urlRegex = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
        return regex(urlRegex, errorMessage, "");
    }

    /**
     * Attempts to parse the field value as a date (ISO-8601).
     *
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} enforcing ISO-8601 date format
     */
    static Rule date(String errorMessage) {
        return Rule.custom(data -> {
            if (data == null || data.isEmpty()) {
                return false;
            }
            try {
                LocalDate.parse(data, DateTimeFormatter.ISO_LOCAL_DATE);
                return true;
            } catch (Exception e) {
                return false;
            }
        }, errorMessage);
    }

    /**
     * Attempts to parse the field value as a date using a custom pattern.
     *
     * @param pattern      The date pattern (e.g. "MM/dd/yyyy")
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} enforcing the specified date format
     */
    static Rule date(String pattern, String errorMessage) {
        return Rule.custom(data -> {
            if (data == null || data.isEmpty()) {
                return false;
            }
            try {
                LocalDate.parse(data, DateTimeFormatter.ofPattern(pattern));
                return true;
            } catch (Exception e) {
                return false;
            }
        }, errorMessage);
    }

    /**
     * Performs a Luhn check on a numeric string to validate credit card numbers
     * (13-19 digits).
     *
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} enforcing a valid credit card number
     */
    static Rule creditCard(String errorMessage) {
        return Rule.custom(data -> {
            if (data == null || data.isEmpty()) {
                return false;
            }
            String digitsOnly = data.replaceAll("[\\D]", "");
            if (digitsOnly.length() < 13 || digitsOnly.length() > 19) {
                return false;
            }
            int sum = 0;
            boolean alternate = false;
            for (int i = digitsOnly.length() - 1; i >= 0; i--) {
                int n = Character.getNumericValue(digitsOnly.charAt(i));
                if (alternate) {
                    n *= 2;
                    if (n > 9)
                        n -= 9;
                }
                sum += n;
                alternate = !alternate;
            }
            return (sum % 10 == 0);
        }, errorMessage);
    }

    /**
     * Requires at least one uppercase letter, one lowercase letter,
     * one digit, one special character, and a minimum length of 8.
     *
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} enforcing "strong" password requirements
     */
    static Rule strongPassword(String errorMessage) {
        return Rule.custom(data -> {
            if (data == null || data.isEmpty() || data.length() < 8) {
                return false;
            }
            boolean hasUpper = false;
            boolean hasLower = false;
            boolean hasDigit = false;
            boolean hasSpecial = false;

            for (char c : data.toCharArray()) {
                if (Character.isUpperCase(c)) {
                    hasUpper = true;
                } else if (Character.isLowerCase(c)) {
                    hasLower = true;
                } else if (Character.isDigit(c)) {
                    hasDigit = true;
                } else {
                    hasSpecial = true;
                }
            }
            return hasUpper && hasLower && hasDigit && hasSpecial;
        }, errorMessage);
    }

    /**
     * Validates a standard IPv4 address.
     *
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} enforcing IPv4 format
     */
    static Rule ipAddress(String errorMessage) {
        return regex("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:\\.(?!$)|$)){4}$", errorMessage, "");
    }

    /**
     * Ensures the numeric value is less than a specified threshold.
     *
     * @param threshold    The upper limit
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} enforcing a maximum numeric value
     */
    static Rule lowerThan(Number threshold, String errorMessage) {
        return Rule.custom(data -> {
            try {
                double value = Double.parseDouble(data);
                return value < threshold.doubleValue();
            } catch (NumberFormatException e) {
                return false;
            }
        }, errorMessage);
    }

    /**
     * Ensures the numeric value is less than or equal to a specified threshold.
     *
     * @param threshold    The upper limit (inclusive)
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} enforcing a maximum numeric value (inclusive)
     */
    static Rule lowerEqualsThan(Number threshold, String errorMessage) {
        return Rule.custom(data -> {
            try {
                double value = Double.parseDouble(data);
                return value <= threshold.doubleValue();
            } catch (NumberFormatException e) {
                return false;
            }
        }, errorMessage);
    }

    /**
     * Ensures the numeric value is greater than a specified threshold.
     *
     * @param threshold    The lower limit
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} enforcing a minimum numeric value
     */
    static Rule greaterThan(Number threshold, String errorMessage) {
        return Rule.custom(data -> {
            try {
                double value = Double.parseDouble(data);
                return value > threshold.doubleValue();
            } catch (NumberFormatException e) {
                return false;
            }
        }, errorMessage);
    }

    /**
     * Ensures the numeric value is greater than or equal to a specified threshold.
     *
     * @param threshold    The lower limit (inclusive)
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} enforcing a minimum numeric value (inclusive)
     */
    static Rule greaterEqualsThan(Number threshold, String errorMessage) {
        return Rule.custom(data -> {
            try {
                double value = Double.parseDouble(data);
                return value >= threshold.doubleValue();
            } catch (NumberFormatException e) {
                return false;
            }
        }, errorMessage);
    }

    /**
     * Ensures the numeric value is between the specified minimum and maximum,
     * inclusive.
     *
     * @param min          The lower bound (inclusive)
     * @param max          The upper bound (inclusive)
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} enforcing a numeric range
     */
    static Rule clamp(Number min, Number max, String errorMessage) {
        return Rule.custom(data -> {
            try {
                double value = Double.parseDouble(data);
                return value >= min.doubleValue() && value <= max.doubleValue();
            } catch (NumberFormatException e) {
                return false;
            }
        }, errorMessage);
    }

    /**
     * Matches the field against a custom regular expression.
     *
     * @param pattern      The regex pattern string
     * @param errorMessage The error message if this rule fails
     * @param modifiers    Optional flags (e.g., {@code "i"} for case-insensitive)
     * @return A {@link Rule} enforcing the specified regex
     */
    static Rule regex(String pattern, String errorMessage, String modifiers) {
        return Rule.custom(data -> {
            if (data == null)
                return false;
            int flags = 0;
            if (modifiers.contains("i")) {
                flags |= Pattern.CASE_INSENSITIVE;
            }
            Pattern p = Pattern.compile(pattern, flags);
            return p.matcher(data).matches();
        }, errorMessage);
    }

    /**
     * A conveniance method for checking a regex pattern without modifiers
     * 
     * @param pattern      The regex pattern string
     * @param errorMessage The error message if this rule fails
     * @return A {@link Rule} enforcing the specified regex
     */
    static Rule regex(String pattern, String errorMessage) {
        return regex(pattern, errorMessage, "");
    }

    /**
     * Helper method to check if a string can be interpreted as numeric.
     */
    private static boolean isNumeric(String data) {
        return data != null && data.matches("-?\\d+(\\.\\d+)?");
    }
}