package io.github.t1willi.openapi.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.t1willi.openapi.annotations.Docs;
import lombok.Getter;

import java.lang.reflect.Type;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
@Getter
public class RequestBodyModel {
    private String description;
    private boolean required;
    private Map<String, ContentModel> content;
    @JsonIgnore
    private Type schemaType;

    public static RequestBodyModel of(Docs doc, ObjectMapper mapper) {
        RequestBodyModel model = new RequestBodyModel();
        model.description = doc.requestDescription();
        model.required = true;
        Type t = doc.requestBody();
        model.schemaType = t;
        model.content = Map.of(
                "application/json",
                new ContentModel(SchemaModel.of(t, mapper)));
        return model;
    }

    @JsonIgnore
    public Type getSchemaType() {
        return schemaType;
    }
}
