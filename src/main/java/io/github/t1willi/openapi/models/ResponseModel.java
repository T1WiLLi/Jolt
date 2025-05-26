package io.github.t1willi.openapi.models;

import java.lang.reflect.Method;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.t1willi.openapi.annotations.ApiResponse;
import lombok.Getter;

@Getter
class ResponseModel {
    private String description;
    private Map<String, ContentModel> content;

    public static ResponseModel of(ApiResponse resp, Method method, ObjectMapper mapper) {
        ResponseModel model = new ResponseModel();
        model.description = resp.description();
        if (resp.schema() != Void.class) {
            model.content = Map.of(
                    "application/json",
                    new ContentModel(SchemaModel.of(resp.schema(), method.getGenericReturnType(), mapper)));
        }
        return model;
    }
}