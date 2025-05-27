package io.github.t1willi.template;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Model class for passing data to templates.
 * This implementation is designed to be immutable during template rendering.
 */
public class JoltModel {
    private final Map<String, Object> model;

    private JoltModel(Map<String, Object> initialData) {
        if (initialData == null) {
            this.model = new HashMap<>();
        } else {
            this.model = new HashMap<>(initialData.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        }
    }

    /**
     * Creates a new empty model.
     * 
     * @return A new empty model
     */
    public static JoltModel empty() {
        return new JoltModel(null);
    }

    /**
     * Creates a new model with initial data.
     * 
     * @param initialData Initial data for the model
     * @return A new model with initial data
     */
    public static JoltModel of(Map<String, Object> initialData) {
        return new JoltModel(initialData);
    }

    /**
     * Creates a new model with a single key-value pair.
     * 
     * @param key   The key of the value
     * @param value The value to be associated with the key
     * @return A new model with the key-value pair
     */
    public static JoltModel of(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("Model key cannot be null");
        }
        if (value == null) {
            return new JoltModel(null);
        }
        return new JoltModel(Map.of(key, value));
    }

    /**
     * Creates a new model with multiple key-value pairs.
     * 
     * @param keyValues The entries to be added to the model
     * @return A new model with the specified entries
     */
    public static JoltModel of(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("key-value pairs must be even in number");
        }

        Map<String, Object> data = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            Object keyObj = keyValues[i];
            Object valueObj = keyValues[i + 1];

            if (keyObj == null || !(keyObj instanceof String)) {
                throw new IllegalArgumentException("keys must be non-null strings");
            }

            String key = (String) keyObj;
            if (valueObj != null) {
                data.put(key, valueObj);
            }
        }
        return new JoltModel(data);
    }

    /**
     * Adds a value to the model.
     * 
     * @param key   The key
     * @param value The value
     * @return This model instance for chaining
     */
    public JoltModel with(String key, Object value) {
        if (key == null) {
            throw new IllegalArgumentException("Model key cannot be null");
        }
        if (value != null) {
            model.put(key, value);
        }
        return this;
    }

    /**
     * Merges another model into this one.
     * Values in the other model will override values in this model.
     * 
     * @param other The model to merge
     * @return This model instance with merged data
     */
    public JoltModel merge(JoltModel other) {
        if (other != null) {
            for (Map.Entry<String, Object> entry : other.model.entrySet()) {
                if (entry.getValue() != null) {
                    model.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return this;
    }

    /**
     * Creates a deep clone of this model.
     * This ensures that modifications to the returned model don't affect this
     * model.
     * 
     * @return A new model with the same data
     */
    public JoltModel clone() {
        return new JoltModel(this.model);
    }

    /**
     * Returns the model as an unmodifiable map.
     * 
     * @return The model as an unmodifiable map
     */
    public Map<String, Object> asMap() {
        return Collections.unmodifiableMap(model);
    }

    /**
     * Gets a value from the model.
     * 
     * @param key The key
     * @return The value or null if not found
     */
    public Object get(String key) {
        return model.get(key);
    }

    /**
     * Gets a typed value from the model.
     * 
     * @param <T>  The expected type
     * @param key  The key
     * @param type The class of the expected type
     * @return The value cast to the expected type, or null if not found
     * @throws ClassCastException if the value cannot be cast to the expected type
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object value = model.get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        throw new ClassCastException("Value for key '" + key + "' is not of type " + type.getName());
    }

    /**
     * Checks if the model contains a key.
     * 
     * @param key The key to check
     * @return true if the model contains the key, false otherwise
     */
    public boolean containsKey(String key) {
        return model.containsKey(key);
    }

    /**
     * Gets all keys in the model.
     * 
     * @return A set of all keys
     */
    public Set<String> getKeys() {
        return Collections.unmodifiableSet(model.keySet());
    }

    /**
     * Gets the size of the model.
     * 
     * @return The number of key-value pairs in the model
     */
    public int size() {
        return model.size();
    }

    /**
     * Checks if the model is empty.
     * 
     * @return true if the model has no key-value pairs, false otherwise
     */
    public boolean isEmpty() {
        return model.isEmpty();
    }

    /**
     * Removes a key-value pair from the model.
     * 
     * @param key The key to remove
     * @return The previous value associated with the key, or null if there was no
     *         mapping
     */
    public Object remove(String key) {
        return model.remove(key);
    }

    /**
     * Clears all key-value pairs from the model.
     * 
     * @return This model instance for chaining
     */
    public JoltModel clear() {
        model.clear();
        return this;
    }
}