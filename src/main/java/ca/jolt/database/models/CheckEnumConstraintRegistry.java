package ca.jolt.database.models;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CheckEnumConstraintRegistry {
    private static final Map<String, String> registry = new ConcurrentHashMap<>();

    public static void register(String constraintName, String allowedValues) {
        registry.put(constraintName, allowedValues);
    }

    public static String getAllowedValues(String constraintName) {
        return registry.get(constraintName);
    }
}
