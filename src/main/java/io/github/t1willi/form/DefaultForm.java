package io.github.t1willi.form;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import io.github.t1willi.exceptions.FormConversionException;
import io.github.t1willi.template.JoltModel;
import io.github.t1willi.utils.JacksonUtil;
import lombok.Getter;

public final class DefaultForm implements Form {
    @Getter
    private final Map<String, String> values;
    private final Map<String, DefaultField> fields = new LinkedHashMap<>();
    private final Map<String, String> first = new LinkedHashMap<>();
    private final Map<String, List<String>> all = new LinkedHashMap<>();

    public DefaultForm(Map<String, String> initialData) {
        this.values = new LinkedHashMap<>(initialData);
    }

    public DefaultForm() {
        this.values = new LinkedHashMap<>();
    }

    @Override
    public Form setValue(String name, String val) {
        values.put(name, val);
        return this;
    }

    @Override
    public Field field(String name) {
        return fields.computeIfAbsent(name, n -> new DefaultField(n, this));
    }

    @Override
    public Form addError(String field, String errorMessage) {
        all.computeIfAbsent(field, k -> new ArrayList<>()).add(errorMessage);
        first.putIfAbsent(field, errorMessage);
        return this;
    }

    @Override
    public boolean validate() {
        first.clear();
        all.clear();
        for (DefaultField f : fields.values()) {
            f.verifyOne();
        }
        return first.isEmpty();
    }

    @Override
    public Map<String, String> errors() {
        return Collections.unmodifiableMap(first);
    }

    @Override
    public Map<String, List<String>> allErrors() {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        all.forEach((k, v) -> copy.put(k, List.copyOf(v)));
        return Collections.unmodifiableMap(copy);
    }

    @Override
    public <T> T buildEntity(TypeReference<T> type, String... ignoreFields) {
        try {
            Map<String, String> data = getFilteredFieldValues(ignoreFields);
            String json = JacksonUtil.getObjectMapper().writeValueAsString(data);
            return JacksonUtil.getObjectMapper().readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new FormConversionException("Failed to build entity from form data.", e);
        }
    }

    @Override
    public <T> T buildEntity(Class<T> type, String... ignoreFields) {
        return buildEntity(new TypeReference<T>() {
            @Override
            public Type getType() {
                return type;
            }
        }, ignoreFields);
    }

    @Override
    public <T> T updateEntity(T entity, String... ignoreFields) {
        try {
            Map<String, String> data = getFilteredFieldValues(ignoreFields);
            JacksonUtil.getObjectMapper().updateValue(entity, data);
            return entity;
        } catch (JsonProcessingException e) {
            throw new FormConversionException("Failed to update entity from form data.", e);
        }
    }

    @Override
    public JoltModel buildModel(String... ignoreFields) {
        Map<String, String> data = getFilteredFieldValues(ignoreFields);
        Map<String, Object> objectData = new HashMap<>(data);
        return JoltModel.from(objectData);
    }

    private Map<String, String> getFilteredFieldValues(String... ignoreFields) {
        Map<String, String> data = new HashMap<>(values);
        for (String field : ignoreFields) {
            data.remove(field);
        }
        return data;
    }
}