package ca.jolt.utils;

public class HelpMethods {

    public static String stackTraceElementToString(StackTraceElement[] stackTraceElements) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : stackTraceElements) {
            sb.append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
