package io.github.t1willi.openapi.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.t1willi.http.ResponseEntity;
import lombok.Getter;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(Include.NON_NULL)
@Getter
public class SchemaModel {
    private String type;
    private String format;
    private SchemaModel items;
    private Map<String, SchemaModel> properties;
    @JsonProperty("$ref")
    private String ref;
    private SchemaModel additionalProperties;

    public static SchemaModel of(Type javaType, ObjectMapper mapper) {
        if (javaType instanceof ParameterizedType pt
                && ResponseEntity.class.equals(pt.getRawType())) {
            return of(pt.getActualTypeArguments()[0], mapper);
        }
        Class<?> raw = mapper.getTypeFactory().constructType(javaType).getRawClass();
        if (Void.class.equals(raw))
            return null;

        if (!isPrimitiveOrContainer(raw)) {
            SchemaModel m = new SchemaModel();
            m.ref = "#/components/schemas/" + raw.getSimpleName();
            return m;
        }
        return buildDefinition(javaType, mapper);
    }

    public static SchemaModel buildDefinition(Type javaType, ObjectMapper mapper) {
        if (javaType instanceof ParameterizedType pt
                && ResponseEntity.class.equals(pt.getRawType())) {
            javaType = pt.getActualTypeArguments()[0];
        }
        Class<?> raw = mapper.getTypeFactory().constructType(javaType).getRawClass();
        if (Void.class.equals(raw))
            return null;

        SchemaModel model = new SchemaModel();
        if (String.class.equals(raw)) {
            model.type = "string";
        } else if (Integer.class.equals(raw) || int.class.equals(raw)) {
            model.type = "integer";
            model.format = "int32";
        } else if (Long.class.equals(raw) || long.class.equals(raw)) {
            model.type = "integer";
            model.format = "int64";
        } else if (Double.class.equals(raw) || double.class.equals(raw)) {
            model.type = "number";
            model.format = "double";
        } else if (Float.class.equals(raw) || float.class.equals(raw)) {
            model.type = "number";
            model.format = "float";
        } else if (Boolean.class.equals(raw) || boolean.class.equals(raw)) {
            model.type = "boolean";
        } else if (raw.isArray()) {
            model.type = "array";
            model.items = buildDefinition(raw.getComponentType(), mapper);
        } else if (Collection.class.isAssignableFrom(raw)) {
            model.type = "array";
            Type elemType = ((ParameterizedType) javaType).getActualTypeArguments()[0];
            model.items = buildDefinition(elemType, mapper);
        } else if (Map.class.isAssignableFrom(raw)) {
            model.type = "object";
            Type valueType = ((ParameterizedType) javaType).getActualTypeArguments()[1];
            model.additionalProperties = buildDefinition(valueType, mapper);
        } else {
            model.type = "object";
            Map<String, SchemaModel> props = new LinkedHashMap<>();
            for (var field : raw.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    props.put(field.getName(),
                            buildDefinition(field.getGenericType(), mapper));
                }
            }
            model.properties = props;
        }
        return model;
    }

    private static boolean isPrimitiveOrContainer(Class<?> raw) {
        return raw.isPrimitive()
                || Number.class.isAssignableFrom(raw)
                || Boolean.class.equals(raw)
                || String.class.equals(raw)
                || raw.isArray()
                || Collection.class.isAssignableFrom(raw)
                || Map.class.isAssignableFrom(raw);
    }
}