package io.github.t1willi.openapi.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.t1willi.http.ResponseEntity;
import io.github.t1willi.openapi.annotations.ApiResponse;
import lombok.Getter;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
@Getter
public class ResponseModel {
    private String description;
    private Map<String, ContentModel> content;
    @JsonIgnore
    private Type contentType;

    public static ResponseModel of(ApiResponse resp, Method method, ObjectMapper mapper) {
        ResponseModel model = new ResponseModel();
        model.description = resp.description();
        if (resp.schema() != Void.class) {
            Type schemaType = resp.schema();
            if (ResponseEntity.class.equals(resp.schema())) {
                schemaType = method.getGenericReturnType();
            }
            model.contentType = schemaType;
            model.content = Map.of(
                    "application/json",
                    new ContentModel(SchemaModel.of(schemaType, mapper)));
        }
        return model;
    }

    @JsonIgnore
    public Type getContentType() {
        return contentType;
    }
}
