package io.github.t1willi.openapi.models;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.t1willi.openapi.annotations.Docs;

@SuppressWarnings("unused")
public final class RequestBodyModel {
    private String description;
    private boolean required;
    private Map<String, MediaTypeModel> content;

    public static RequestBodyModel of(Docs docs, ObjectMapper mapper) {
        if (docs.requestBody() == Void.class && docs.requestDescription().isEmpty()) {
            return null;
        }
        RequestBodyModel model = new RequestBodyModel();
        model.description = docs.requestDescription().isEmpty() ? null : docs.requestDescription();
        model.required = docs.requestBody() != Void.class;
        model.content = docs.requestBody() != Void.class
                ? Map.of("application/json", MediaTypeModel.of(docs.requestBody(), mapper))
                : null;
        return model;
    }
}
