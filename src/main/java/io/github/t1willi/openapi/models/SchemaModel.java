package io.github.t1willi.openapi.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.github.t1willi.http.ResponseEntity;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class SchemaModel {
    String type;
    String format;
    SchemaModel items;
    Map<String, SchemaModel> properties;
    String ref;
    SchemaModel additionalProperties;

    public static SchemaModel of(Class<?> type, ObjectMapper mapper) {
        if (type == null || type == Void.class) {
            return null;
        }

        SchemaModel model = new SchemaModel();

        if (type == ResponseEntity.class) {
            model.type = "object";
            return model;
        }

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
        } else if (type.isArray()) {
            model.type = "array";
            Class<?> componentType = type.getComponentType();
            model.items = of(componentType, mapper);
        } else if (Collection.class.isAssignableFrom(type)) {
            model.type = "array";
            Class<?> componentType = Object.class; // Default
            try {
                componentType = mapper.getTypeFactory()
                        .constructCollectionType(List.class, type)
                        .getContentType()
                        .getRawClass();
            } catch (Exception e) {
                // Fallback and ignore
            }
            model.items = of(componentType, mapper);
        } else if (Map.class.isAssignableFrom(type)) {
            model.type = "object";
            Class<?> valueType = Object.class;
            try {
                valueType = mapper.getTypeFactory()
                        .constructMapType(Map.class, String.class, Object.class)
                        .getContentType()
                        .getRawClass();
            } catch (Exception e) {
                // Fallback to Object
            }
            model.additionalProperties = of(valueType, mapper);
        } else {
            model.type = "object";
            model.ref = "#/components/schemas/" + type.getSimpleName();
            model.properties = new LinkedHashMap<>();
            try {
                for (Field field : type.getDeclaredFields()) {
                    if (!Modifier.isStatic(field.getModifiers())) {
                        model.properties.put(field.getName(), of(field.getType(), mapper));
                    }
                }
            } catch (Exception e) {
                model.properties = null;
            }
        }

        return model;
    }

    public static SchemaModel of(Class<?> type, java.lang.reflect.Type genericType, ObjectMapper mapper) {
        if (type == null || type == Void.class) {
            return null;
        }

        if (type == ResponseEntity.class && genericType instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType) genericType;
            java.lang.reflect.Type[] typeArgs = paramType.getActualTypeArguments();
            if (typeArgs.length > 0) {
                Class<?> innerType = TypeFactory.rawClass(typeArgs[0]);
                return of(innerType, typeArgs[0], mapper);
            }
        }

        return of(type, mapper);
    }
}