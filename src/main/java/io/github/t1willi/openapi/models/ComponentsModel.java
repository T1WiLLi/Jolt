package io.github.t1willi.openapi.models;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@JsonInclude(Include.NON_NULL)
@NoArgsConstructor
@Setter
public final class ComponentsModel {
    Map<String, SchemaModel> schemas;

    ComponentsModel(Map<String, SchemaModel> schemas) {
        this.schemas = schemas;
    }
}
