package io.github.t1willi.openapi.models;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.t1willi.openapi.annotations.ApiParameter;
import lombok.Getter;

@Getter
public final class ParameterModel {
    private String name;
    private String in;
    private String description;
    private boolean required;
    private SchemaModel schema;
    private String example;

    public static ParameterModel of(ApiParameter param, ObjectMapper mapper) {
        ParameterModel model = new ParameterModel();
        model.name = param.name();
        model.in = param.in().name().toLowerCase();
        model.description = param.description().isEmpty() ? null : param.description();
        model.required = param.required();
        model.schema = SchemaModel.of(param.type(), mapper);
        model.example = param.example().isEmpty() ? null : param.example();
        return model;
    }
}
