package io.github.t1willi.openapi.models;

import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@Getter
public class ContentModel {
    private SchemaModel schema;

    public ContentModel(SchemaModel schema) {
        this.schema = schema;
    }
}
