package io.github.t1willi.openapi.models;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.t1willi.openapi.annotations.Docs;
import lombok.Getter;

@Getter
public final class RequestBodyModel {
    private String description;
    private boolean required;
    private Map<String, ContentModel> content;

    public static RequestBodyModel of(Docs doc, ObjectMapper mapper) {
        RequestBodyModel model = new RequestBodyModel();
        model.description = doc.requestDescription();
        model.required = true;
        model.content = Map.of(
                "application/json",
                new ContentModel(SchemaModel.of(doc.requestBody(), mapper)));
        return model;
    }
}
