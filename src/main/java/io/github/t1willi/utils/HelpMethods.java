package io.github.t1willi.utils;

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

        String clean = path.startsWith("/") ? path.substring(1) : path;

        if (clean.contains("../") || clean.contains("..\\") ||
                clean.startsWith("../") || clean.startsWith("..\\") ||
                clean.contains("/..") || clean.contains("\\..")) {
            return false;
        }

        return clean.matches("^[a-zA-Z0-9_\\-./]+$");
    }
}
