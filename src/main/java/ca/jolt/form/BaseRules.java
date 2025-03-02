package ca.jolt.form;

import java.util.regex.Pattern;

final class BaseRules {

    BaseRules() {
        // Prevent instantiation.
    }

    static Rule required(String errorMessage) {
        return Rule.custom((data, allValues) -> {
            if (data == null)
                return false;
            if (isNumeric(data))
                return true;
            return !data.trim().isEmpty();
        }, errorMessage);
    }

    static Rule minLength(int min, String errorMessage) {
        return Rule.custom((data, allValues) -> data != null && data.length() >= min, errorMessage);
    }

    static Rule maxLength(int max, String errorMessage) {
        return Rule.custom((data, allValues) -> data != null && data.length() <= max, errorMessage);
    }

    static Rule email(String errorMessage) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return regex(emailRegex, errorMessage, "");
    }

    static Rule alphanumeric(String errorMessage) {
        return regex("^[a-zA-Z0-9]+$", errorMessage, "");
    }

    static Rule phoneNumber(String errorMessage) {
        return regex("^\\+?[0-9\\s-()]{7,20}$", errorMessage, "");
    }

    static Rule zipCode(String errorMessage) {
        return regex("^[0-9]{5}(?:-[0-9]{4})?$", errorMessage, "");
    }

    static Rule url(String errorMessage) {
        String urlRegex = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
        return regex(urlRegex, errorMessage, "");
    }

    static Rule date(String errorMessage) {
        return Rule.custom((data, allValues) -> {
            if (data == null || data.isEmpty())
                return false;
            try {
                java.time.LocalDate.parse(data, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                return true;
            } catch (Exception e) {
                return false;
            }
        }, errorMessage);
    }

    static Rule date(String pattern, String errorMessage) {
        return Rule.custom((data, allValues) -> {
            if (data == null || data.isEmpty())
                return false;
            try {
                java.time.LocalDate.parse(data, java.time.format.DateTimeFormatter.ofPattern(pattern));
                return true;
            } catch (Exception e) {
                return false;
            }
        }, errorMessage);
    }

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

    static Rule ipAddress(String errorMessage) {
        return regex("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:\\.(?!$)|$)){4}$", errorMessage, "");
    }

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

    private static boolean isNumeric(String data) {
        return data != null && data.matches("-?\\d+(\\.\\d+)?");
    }
}
