package io.github.t1willi.openapi.models;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("unused")
public final class SchemaModel {
    private String type;
    private String format;
    private String ref;
    private Map<String, SchemaModel> properties;
    private List<String> required;
    private SchemaModel items;

    public static SchemaModel of(Class<?> type, ObjectMapper mapper) {
        if (type == Void.class) {
            return null;
        }
        SchemaModel model = new SchemaModel();

        if (type == String.class) {
            model.type = "string";
        } else if (type == Integer.class || type == int.class) {
            model.type = "integer";
            model.format = "int32";
        } else if (type == Long.class || type == long.class) {
            model.type = "integer";
            model.format = "int64";
        } else if (type == Double.class || type == double.class) {
            model.type = "number";
            model.format = "double";
        } else if (type == Float.class || type == float.class) {
            model.type = "number";
            model.format = "float";
        } else if (type == Boolean.class || type == boolean.class) {
            model.type = "boolean";
        } else if (type.isArray() || Collection.class.isAssignableFrom(type)) {
            model.type = "array";
            Class<?> componentType = type.isArray() ? type.getComponentType()
                    : mapper.getTypeFactory().constructCollectionType(List.class, type).getContentType().getRawClass();
            model.items = of(componentType, mapper);
        } else if (Map.class.isAssignableFrom(type)) {
            model.type = "object";
            Class<?> valueType = mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
                    .getContentType().getRawClass();
            model.properties = Map.of("additionalProperties", of(valueType, mapper));
        } else {
            model.type = "object";
            model.ref = "#/components/schemas/" + type.getSimpleName();
            try {
                model.properties = new LinkedHashMap<>();
                for (var field : type.getDeclaredFields()) {
                    if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        model.properties.put(field.getName(), of(field.getType(), mapper));
                    }
                }
            } catch (Exception e) {
                model.properties = null;
            }
        }
        return model;
    }
}