package io.github.t1willi.form;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import io.github.t1willi.template.JoltModel;

//TODO: Add funmction to add error to the form. :)
public interface Form {
    Field field(String name);

    boolean validate();

    Form addError(String field, String errorMessage);

    Map<String, String> errors();

    Map<String, List<String>> allErrors();

    Form setValue(String name, String val);

    <T> T buildEntity(TypeReference<T> type, String... ignoreFields);

    <T> T buildEntity(Class<T> type, String... ignoreFields);

    <T> T updateEntity(T entity, String... ignoreFields);

    JoltModel buildModel(String... ignoreFields);
}
