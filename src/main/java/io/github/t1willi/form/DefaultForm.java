package io.github.t1willi.form;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    void addError(String field, String msg) {
        all.computeIfAbsent(field, k -> new ArrayList<>()).add(msg);
        first.putIfAbsent(field, msg);
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
}