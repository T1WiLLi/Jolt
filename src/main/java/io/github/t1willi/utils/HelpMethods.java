package io.github.t1willi.utils;

import io.github.t1willi.exceptions.JoltDIException;

public class HelpMethods {

    public static String stackTraceElementToString(StackTraceElement[] stackTraceElements) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : stackTraceElements) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }

    public static boolean isValidStaticResourcePath(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        String normalized = path;
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }

        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        if (normalized.contains("..")) {
            return false;
        }

        return normalized.matches("^[a-zA-Z0-9_\\-./]+$");
    }

    public static Object convert(String raw, Class<?> targetType) {
        if (raw == null)
            return null;
        if (targetType == String.class)
            return raw;
        if (targetType == int.class || targetType == Integer.class)
            return Integer.parseInt(raw);
        if (targetType == long.class || targetType == Long.class)
            return Long.parseLong(raw);
        if (targetType == double.class || targetType == Double.class)
            return Double.parseDouble(raw);
        if (targetType == boolean.class || targetType == Boolean.class)
            return Boolean.parseBoolean(raw);
        if (targetType == float.class || targetType == Float.class)
            return Float.parseFloat(raw);
        if (targetType == short.class || targetType == Short.class)
            return Short.parseShort(raw);
        if (targetType == byte.class || targetType == Byte.class)
            return Byte.parseByte(raw);
        if (targetType == char.class || targetType == Character.class) {
            if (raw.length() != 1)
                throw new JoltDIException("Cannot convert to char: " + raw);
            return raw.charAt(0);
        }
        throw new JoltDIException("Cannot convert String to " + targetType.getSimpleName());
    }

    public static Object smartParse(Object value) {
        if (value == null) {
            return null;
        }

        if (!(value instanceof String)) {
            return value;
        }

        String trimmedValue = ((String) value).trim();
        if (trimmedValue.isEmpty()) {
            return "";
        }

        if (trimmedValue.equalsIgnoreCase("null")) {
            return null;
        }

        if (trimmedValue.equalsIgnoreCase("true") || trimmedValue.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(trimmedValue);
        }

        try {
            if (!trimmedValue.contains(".") && !trimmedValue.toLowerCase().contains("e")) {
                return Integer.parseInt(trimmedValue);
            }
        } catch (NumberFormatException e) {
            // Not an integer, continue
        }

        try {
            return Double.parseDouble(trimmedValue);
        } catch (NumberFormatException e) {
            // Not a double, continue
        }

        return trimmedValue;
    }

    public static boolean equivalentValues(Object value1, Object value2) {
        if (value1 == null && value2 == null) {
            return true;
        }
        if (value1 == null || value2 == null) {
            return false;
        }

        if (value1.getClass().equals(value2.getClass())) {
            return value1.equals(value2);
        }

        if (value1 instanceof String && !(value2 instanceof String)) {
            Object parsed = smartParse((String) value1);
            return parsed.equals(value2);
        }
        if (value2 instanceof String && !(value1 instanceof String)) {
            Object parsed = smartParse((String) value2);
            return value1.equals(parsed);
        }

        return value1.toString().equals(value2.toString());
    }
}
