package io.github.t1willi.openapi.models;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@Getter
public final class MediaTypeModel {
    private SchemaModel schema;

    public static MediaTypeModel of(Class<?> schemaType, ObjectMapper mapper) {
        if (schemaType == Void.class) {
            return null;
        }
        MediaTypeModel model = new MediaTypeModel();
        model.schema = SchemaModel.of(schemaType, mapper);
        return model;
    }
}
