package io.github.t1willi.openapi.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.t1willi.openapi.annotations.Docs;
import io.github.t1willi.openapi.annotations.OpenApi;
import lombok.Getter;
import io.github.t1willi.annotations.Mapping;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public final class OpenApiModel {
    private String openapi = "3.1.0";
    private InfoModel info;
    private Map<String, PathItemModel> paths;

    public static OpenApiModel of(OpenApi openApi, List<Map.Entry<Docs, Method>> docs) {
        ObjectMapper mapper = new ObjectMapper();
        OpenApiModel model = new OpenApiModel();
        model.info = InfoModel.of(openApi);
        model.paths = new HashMap<>();

        for (Map.Entry<Docs, Method> entry : docs) {
            Docs doc = entry.getKey();
            Method method = entry.getValue();
            String path = "/endpoint";
            if (method != null && method.isAnnotationPresent(Mapping.class)) {
                String mappingPath = method.getAnnotation(Mapping.class).value();
                path = mappingPath.isEmpty() ? "/endpoint"
                        : mappingPath.startsWith("/") ? mappingPath : "/" + mappingPath;
            } else if (!doc.operationId().isEmpty()) {
                path = "/" + doc.operationId().toLowerCase();
            }
            model.paths.computeIfAbsent(path, k -> PathItemModel.of(doc, method, mapper));
        }
        return model;
    }
}