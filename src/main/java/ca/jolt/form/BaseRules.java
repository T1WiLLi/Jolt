package ca.jolt.form;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * A utility class containing static factory methods that produce common
 * {@link Rule} instances. This class offers prebuilt rules such as:
 * <ul>
 * <li>{@link #required(String)} for non-empty fields.</li>
 * <li>{@link #minLength(int, String)} and {@link #maxLength(int, String)}
 * for length constraints.</li>
 * <li>{@link #email(String)} for email format checking.</li>
 * <li>{@link #phoneNumber(String)} for phone format checking.</li>
 * <li>{@link #creditCard(String)} for Luhn algorithm checks.</li>
 * <li>{@link #regex(String, String, String)} for a custom regular expression
 * rule.</li>
 * <li>And more, including numeric comparisons and date parsing checks.</li>
 * </ul>
 *
 * <p>
 * This class is not meant to be instantiated—its methods should be called
 * directly (e.g., {@code BaseRules.required("Field is required")}).
 * </p>
 *
 * @see Rule
 * @see Form
 * @see FieldValidator
 * @author William Beaudin
 * @since 1.0
 */
final class BaseRules {

    /**
     * Private constructor to prevent instantiation.
     */
    BaseRules() {
        // Prevent instantiation.
    }

    /**
     * Produces a rule ensuring the field is neither {@code null} nor empty.
     */
    static Rule required(String errorMessage) {
        return Rule.custom((data, allValues) -> {
            if (data == null)
                return false;
            // If numeric, allow "0" or "0.0" etc.
            if (isNumeric(data))
                return true;
            return !data.trim().isEmpty();
        }, errorMessage);
    }

    /**
     * Produces a rule ensuring the field is at least {@code min} characters.
     */
    static Rule minLength(int min, String errorMessage) {
        return Rule.custom((data, allValues) -> data != null && data.length() >= min, errorMessage);
    }

    /**
     * Produces a rule ensuring the field is at most {@code max} characters.
     */
    static Rule maxLength(int max, String errorMessage) {
        return Rule.custom((data, allValues) -> data != null && data.length() <= max, errorMessage);
    }

    /**
     * Produces a rule verifying valid email format via a regular expression.
     */
    static Rule email(String errorMessage) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return regex(emailRegex, errorMessage, "");
    }

    /**
     * Produces a rule verifying only alphanumeric characters.
     */
    static Rule alphanumeric(String errorMessage) {
        return regex("^[a-zA-Z0-9]+$", errorMessage, "");
    }

    /**
     * Produces a rule verifying phone numbers that may include plus signs,
     * spaces, dashes, and parentheses (7-20 digits in length).
     */
    static Rule phoneNumber(String errorMessage) {
        return regex("^\\+?[0-9\\s-()]{7,20}$", errorMessage, "");
    }

    /**
     * Produces a rule verifying US zip codes (5-digit or 5+4-digit format).
     */
    static Rule zipCode(String errorMessage) {
        return regex("^[0-9]{5}(?:-[0-9]{4})?$", errorMessage, "");
    }

    /**
     * Produces a rule verifying simple URL format (supports http, https, ftp).
     */
    static Rule url(String errorMessage) {
        String urlRegex = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
        return regex(urlRegex, errorMessage, "");
    }

    /**
     * Produces a rule that attempts to parse the field value as a date using
     * the ISO-8601 pattern.
     */
    static Rule date(String errorMessage) {
        return Rule.custom((data, allValues) -> {
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
     * Produces a rule that attempts to parse the field value as a date using
     * the provided pattern.
     */
    static Rule date(String pattern, String errorMessage) {
        return Rule.custom((data, allValues) -> {
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
     * Produces a rule ensuring that the field value passes a Luhn check
     * for credit card numbers (13-19 digits).
     */
    static Rule creditCard(String errorMessage) {
        return Rule.custom((data, allValues) -> {
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
     * Produces a rule ensuring a "strong" password with at least one
     * uppercase letter, one lowercase letter, one digit, one special character,
     * and a minimum length of 8 characters.
     */
    static Rule strongPassword(String errorMessage) {
        return Rule.custom((data, allValues) -> {
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
     * Produces a rule verifying a valid IPv4 address format.
     */
    static Rule ipAddress(String errorMessage) {
        return regex("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:\\.(?!$)|$)){4}$", errorMessage, "");
    }

    /**
     * Produces a rule ensuring the numeric value is less than the specified
     * threshold.
     */
    static Rule lowerThan(Number threshold, String errorMessage) {
        return Rule.custom((data, allValues) -> {
            try {
                double value = Double.parseDouble(data);
                return value < threshold.doubleValue();
            } catch (NumberFormatException e) {
                return false;
            }
        }, errorMessage);
    }

    /**
     * Produces a rule ensuring the numeric value is less than or equal to the
     * specified threshold.
     */
    static Rule lowerEqualsThan(Number threshold, String errorMessage) {
        return Rule.custom((data, allValues) -> {
            try {
                double value = Double.parseDouble(data);
                return value <= threshold.doubleValue();
            } catch (NumberFormatException e) {
                return false;
            }
        }, errorMessage);
    }

    /**
     * Produces a rule ensuring the numeric value is greater than the specified
     * threshold.
     */
    static Rule greaterThan(Number threshold, String errorMessage) {
        return Rule.custom((data, allValues) -> {
            try {
                double value = Double.parseDouble(data);
                return value > threshold.doubleValue();
            } catch (NumberFormatException e) {
                return false;
            }
        }, errorMessage);
    }

    /**
     * Produces a rule ensuring the numeric value is greater than or equal to the
     * specified threshold.
     */
    static Rule greaterEqualsThan(Number threshold, String errorMessage) {
        return Rule.custom((data, allValues) -> {
            try {
                double value = Double.parseDouble(data);
                return value >= threshold.doubleValue();
            } catch (NumberFormatException e) {
                return false;
            }
        }, errorMessage);
    }

    /**
     * Produces a rule verifying the field matches a specified regular expression.
     *
     * @param pattern
     *                     The regex pattern string.
     * @param errorMessage
     *                     The error message on match failure.
     * @param modifiers
     *                     Optional string of regex flags (e.g., "i" for
     *                     case-insensitivity).
     */
    static Rule regex(String pattern, String errorMessage, String modifiers) {
        return Rule.custom((data, allValues) -> {
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
     * Helper method to check if a string can be interpreted as numeric.
     */
    private static boolean isNumeric(String data) {
        return data != null && data.matches("-?\\d+(\\.\\d+)?");
    }
}