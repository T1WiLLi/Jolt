package io.github.t1willi.security.utils;

import java.util.regex.Pattern;
import java.util.Map;
import java.util.HashMap;

/**
 * Utility class for sanitizing input data to prevent injection attacks.
 * This class is internal to the JoltContext and not exposed to users.
 */
public class InputSanitizer {
    public static final Pattern FILENAME_SANITIZE_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");
    public static final Pattern HTML_SCRIPT_PATTERN = Pattern.compile("<(script|iframe|object|embed|form)[^>]*>",
            Pattern.CASE_INSENSITIVE);
    public static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "('|\")?\\s*(OR|AND|UNION|SELECT|INSERT|UPDATE|DELETE|DROP|ALTER)\\s",
            Pattern.CASE_INSENSITIVE);
    public static final Pattern JSON_SANITIZE_PATTERN = Pattern.compile("[\u0000-\u001F\u2028\u2029]");

    /**
     * Sanitizes a filename to prevent path traversal attacks.
     * 
     * @param filename The original filename
     * @return A sanitized filename
     */
    public static String sanitizeFilename(String filename) {
        if (filename == null) {
            return null;
        }

        String sanitized = filename.replaceAll("\\.\\./", "")
                .replaceAll("\\.\\.\\\\", "");
        sanitized = FILENAME_SANITIZE_PATTERN.matcher(sanitized).replaceAll("_");

        return sanitized;
    }

    /**
     * Sanitizes a string value to prevent XSS attacks.
     * 
     * @param value The original string value
     * @return A sanitized string
     */
    public static String sanitizeString(String value) {
        if (value == null) {
            return null;
        }
        String sanitized = value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
        sanitized = HTML_SCRIPT_PATTERN.matcher(sanitized).replaceAll("");
        sanitized = SQL_INJECTION_PATTERN.matcher(sanitized).replaceAll(" ");
        return sanitized;
    }

    /**
     * Sanitizes a string value that will be used in JSON to prevent JSON injection.
     * 
     * @param value The original string value
     * @return A sanitized string safe for JSON inclusion
     */
    public static String sanitizeJsonString(String value) {
        if (value == null) {
            return null;
        }
        return JSON_SANITIZE_PATTERN.matcher(value).replaceAll("");
    }

    /**
     * Sanitizes a map of parameter values.
     * 
     * @param params The original parameter map
     * @return A sanitized parameter map
     */
    public static Map<String, String> sanitizeParams(Map<String, String> params) {
        if (params == null) {
            return null;
        }

        Map<String, String> sanitizedParams = new HashMap<>();
        params.forEach((key, value) -> {
            sanitizedParams.put(sanitizeString(key), sanitizeString(value));
        });

        return sanitizedParams;
    }
}
