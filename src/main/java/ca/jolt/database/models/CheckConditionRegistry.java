package ca.jolt.database.models;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A registry to store check constraint conditions by constraint name.
 */
public final class CheckConditionRegistry {

    // Maps constraintName -> the condition (string) describing the constraint
    private static final Map<String, String> REGISTRY = new ConcurrentHashMap<>();

    private CheckConditionRegistry() {
        /* no-op */
    }

    public static void register(String constraintName, String condition) {
        REGISTRY.put(constraintName.toLowerCase(), condition);
    }

    public static String getCondition(String constraintName) {
        REGISTRY.forEach((key, value) -> System.out.println("Key: " + key + ", Value: " + value));
        return REGISTRY.get(constraintName.toLowerCase());
    }
}
