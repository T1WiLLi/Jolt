package io.github.t1willi.openapi.models;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.t1willi.openapi.annotations.ApiResponse;

@SuppressWarnings("unused")
public final class ReponseModel {
    private String description;
    private Map<String, MediaTypeModel> content;

    public static ReponseModel of(ApiResponse response, ObjectMapper mapper) {
        ReponseModel model = new ReponseModel();
        model.description = response.description();
        model.content = response.schema() != Void.class
                ? Map.of(response.contentType(), MediaTypeModel.of(response.schema(), mapper))
                : null;
        return model;
    }
}
