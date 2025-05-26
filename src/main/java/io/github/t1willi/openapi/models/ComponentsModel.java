package io.github.t1willi.openapi.models;

import java.util.Map;

import lombok.Getter;

@Getter
public final class ComponentsModel {
    Map<String, SchemaModel> schemas;

    ComponentsModel(Map<String, SchemaModel> schemas) {
        this.schemas = schemas;
    }
}
