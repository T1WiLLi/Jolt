package io.github.t1willi.openapi.models;

import lombok.Getter;

@Getter
public class ContentModel {
    private SchemaModel schema;

    public ContentModel(SchemaModel schema) {
        this.schema = schema;
    }
}
