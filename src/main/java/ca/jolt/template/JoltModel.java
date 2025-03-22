package ca.jolt.template;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides a fluent interface for building data models to be used with
 * Freemarker templates.
 * <p>
 * This class wraps a map of values and provides convenient methods for adding
 * values
 * to the model.
 *
 * @since 1.0
 */
public class JoltModel {

    private final Map<String, Object> values = new HashMap<>();

    /**
     * Creates a new empty template model.
     *
     * @return A new template model instance
     */
    public static JoltModel create() {
        return new JoltModel();
    }

    /**
     * Creates a new template model with the specified key-value pair.
     *
     * @param key   The key
     * @param value The value
     * @return A new template model instance
     */
    public static JoltModel of(String key, Object value) {
        return new JoltModel().with(key, value);
    }

    /**
     * Creates a new template model from the specified map.
     *
     * @param map The map of values
     * @return A new template model instance
     */
    public static JoltModel from(Map<String, Object> map) {
        JoltModel model = new JoltModel();
        model.values.putAll(map);
        return model;
    }

    /**
     * Adds a value to the model with the specified key.
     *
     * @param key   The key
     * @param value The value
     * @return This model instance for method chaining
     */
    public JoltModel with(String key, Object value) {
        values.put(key, value);
        return this;
    }

    /**
     * Adds all entries from the specified map to the model.
     *
     * @param map The map of values to add
     * @return This model instance for method chaining
     */
    public JoltModel withAll(Map<String, Object> map) {
        values.putAll(map);
        return this;
    }

    /**
     * Merges another template model into this one.
     *
     * @param other The other template model to merge
     * @return This model instance for method chaining
     */
    public JoltModel merge(JoltModel other) {
        values.putAll(other.values);
        return this;
    }

    /**
     * Gets the underlying map of values.
     *
     * @return The map of values
     */
    public Map<String, Object> asMap() {
        return new HashMap<>(values);
    }

    /**
     * Checks if the model contains a value with the specified key.
     *
     * @param key The key to check
     * @return True if the model contains the key, false otherwise
     */
    public boolean has(String key) {
        return values.containsKey(key);
    }

    /**
     * Gets the value associated with the specified key.
     *
     * @param key The key
     * @return The value, or null if the key does not exist
     */
    public Object get(String key) {
        return values.get(key);
    }

    /**
     * Removes the value associated with the specified key.
     *
     * @param key The key
     * @return This model instance for method chaining
     */
    public JoltModel remove(String key) {
        values.remove(key);
        return this;
    }

    /**
     * Clears all values from the model.
     *
     * @return This model instance for method chaining
     */
    public JoltModel clear() {
        values.clear();
        return this;
    }
}