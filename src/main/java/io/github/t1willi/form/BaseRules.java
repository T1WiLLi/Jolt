package io.github.t1willi.form;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.github.t1willi.utils.JacksonUtil;

final class BaseRules {
    private BaseRules() {
        // No-Op
    }

    static Rule required(String msg) {
        return Rule.custom(data -> data != null && !data.trim().isEmpty(), msg);
    }

    static <T> Rule type(Class<T> type, String msg) {
        return Rule.custom(data -> {
            if (data == null || data.trim().isEmpty()) {
                return false;
            }
            var mapper = JacksonUtil.getObjectMapper();
            String trimmed = data.trim();

            try {
                if (type == String.class) {
                    mapper.readValue(mapper.writeValueAsString(trimmed), type);
                } else {
                    mapper.readValue(trimmed, type);
                }
                return true;
            } catch (JsonProcessingException e) {
                return false;
            }
        }, msg);
    }

    static Rule min(Number min, String msg) {
        return Rule.custom(data -> {
            if (data == null || data.isEmpty())
                return false;
            try {
                return Double.parseDouble(data) >= min.doubleValue();
            } catch (NumberFormatException e) {
                return data.length() >= min.intValue();
            }
        }, msg);
    }

    static Rule max(Number max, String msg) {
        return Rule.custom(data -> {
            if (data == null || data.isEmpty())
                return false;
            try {
                return Double.parseDouble(data) <= max.doubleValue();
            } catch (NumberFormatException e) {
                return data.length() <= max.intValue();
            }
        }, msg);
    }

    static Rule email(String msg) {
        String rx = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return regex(rx, msg, "");
    }

    static Rule alphanumeric(String msg) {
        return regex("^[a-zA-Z0-9]+$", msg, "");
    }

    static Rule phoneNumber(String msg) {
        return regex("^\\+?[0-9\\s-()]{7,20}$", msg, "");
    }

    static Rule zipCode(String msg) {
        return regex("^[0-9]{5}(?:-[0-9]{4})?$", msg, "");
    }

    static Rule url(String msg) {
        String rx = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
        return regex(rx, msg, "");
    }

    static Rule date(String msg) {
        return Rule.custom(data -> {
            if (data == null || data.isEmpty())
                return false;
            try {
                LocalDate.parse(data, DateTimeFormatter.ISO_LOCAL_DATE);
                return true;
            } catch (Exception e) {
                return false;
            }
        }, msg);
    }

    static Rule date(String pat, String msg) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pat);
        return Rule.custom(data -> {
            if (data == null || data.isEmpty())
                return false;
            try {
                LocalDate.parse(data, fmt);
                return true;
            } catch (Exception e) {
                return false;
            }
        }, msg);
    }

    static Rule creditCard(String msg) {
        return Rule.custom(data -> {
            if (data == null || data.isEmpty())
                return false;
            String d = data.replaceAll("[^0-9]", "");
            if (d.length() < 13 || d.length() > 19)
                return false;
            int sum = 0;
            boolean alt = false;
            for (int i = d.length() - 1; i >= 0; i--) {
                int n = Character.getNumericValue(d.charAt(i));
                if (alt) {
                    n *= 2;
                    if (n > 9)
                        n -= 9;
                }
                sum += n;
                alt = !alt;
            }
            return sum % 10 == 0;
        }, msg);
    }

    static Rule strongPassword(String msg) {
        return Rule.custom(data -> {
            if (data == null || data.length() < 8)
                return false;
            boolean up = false, lo = false, dig = false, sp = false;
            for (char c : data.toCharArray()) {
                if (Character.isUpperCase(c))
                    up = true;
                else if (Character.isLowerCase(c))
                    lo = true;
                else if (Character.isDigit(c))
                    dig = true;
                else
                    sp = true;
            }
            return up && lo && dig && sp;
        }, msg);
    }

    static Rule ipAddress(String msg) {
        return regex("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)(?:\\.(?!$)|$)){4}$", msg, "");
    }

    static Rule lowerThan(Number thr, String msg) {
        return Rule.custom(data -> {
            try {
                return Double.parseDouble(data) < thr.doubleValue();
            } catch (Exception e) {
                return false;
            }
        }, msg);
    }

    static Rule lowerEqualsThan(Number thr, String msg) {
        return Rule.custom(data -> {
            try {
                return Double.parseDouble(data) <= thr.doubleValue();
            } catch (Exception e) {
                return false;
            }
        }, msg);
    }

    static Rule greaterThan(Number thr, String msg) {
        return Rule.custom(data -> {
            try {
                return Double.parseDouble(data) > thr.doubleValue();
            } catch (Exception e) {
                return false;
            }
        }, msg);
    }

    static Rule greaterEqualsThan(Number thr, String msg) {
        return Rule.custom(data -> {
            try {
                return Double.parseDouble(data) >= thr.doubleValue();
            } catch (Exception e) {
                return false;
            }
        }, msg);
    }

    static Rule clamp(Number min, Number max, String msg) {
        return Rule.custom(data -> {
            try {
                double v = Double.parseDouble(data);
                return v >= min.doubleValue() && v <= max.doubleValue();
            } catch (Exception e) {
                return false;
            }
        }, msg);
    }

    static Rule regex(String pat, String msg, String mods) {
        return Rule.custom(data -> {
            if (data == null) {
                return false;
            }
            int f = mods.contains("i") ? Pattern.CASE_INSENSITIVE : 0;
            return Pattern.compile(pat, f).matcher(data).matches();
        }, msg);
    }
}
