package io.github.t1willi.openapi.models;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.t1willi.openapi.annotations.ApiParameter;
import lombok.Getter;

@Getter
class ParameterModel {
    private String name;
    private String in;
    private String description;
    private boolean required;
    private SchemaModel schema;
    private Object example;

    public static ParameterModel of(ApiParameter param, ObjectMapper mapper) {
        ParameterModel model = new ParameterModel();
        model.name = param.name();
        model.in = param.in().toString().toLowerCase();
        model.description = param.description();
        model.required = param.required();
        model.schema = SchemaModel.of(param.type(), mapper);
        model.example = param.example().isEmpty() ? null : param.example();
        return model;
    }
}
