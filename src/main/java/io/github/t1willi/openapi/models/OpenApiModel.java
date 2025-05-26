package io.github.t1willi.openapi.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.t1willi.annotations.Controller;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.annotations.Post;
import io.github.t1willi.openapi.annotations.Docs;
import io.github.t1willi.openapi.annotations.OpenApi;
import lombok.Getter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public final class OpenApiModel {
    private String openapi = "3.0.3";
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
            String path = getPath(method, doc);
            model.paths.computeIfAbsent(path, k -> PathItemModel.of(doc, method, mapper));
        }
        return model;
    }

    private static String getPath(Method method, Docs doc) {
        String basePath = getBasePath(method.getDeclaringClass());
        String endpointPath = "/endpoint";

        if (method.isAnnotationPresent(Get.class)) {
            Get get = method.getAnnotation(Get.class);
            String value = get.value().isEmpty() ? "" : get.value();
            endpointPath = value.startsWith("/") ? value : "/" + value;
        } else if (method.isAnnotationPresent(Post.class)) {
            Post post = method.getAnnotation(Post.class);
            String value = post.value().isEmpty() ? "" : post.value();
            endpointPath = value.startsWith("/") ? value : "/" + value;
        } else if (!doc.operationId().isEmpty()) {
            endpointPath = "/" + doc.operationId().toLowerCase();
        }

        return basePath + (endpointPath.equals("/") ? "" : endpointPath);
    }

    private static String getBasePath(Class<?> controllerClass) {
        if (controllerClass.isAnnotationPresent(Controller.class)) {
            String path = controllerClass.getAnnotation(Controller.class).value();
            if (path.contains("[controller]")) {
                String controllerName = controllerClass.getSimpleName().replace("Controller", "").toLowerCase();
                path = path.replace("[controller]", controllerName);
            }
            return path.startsWith("/") ? path : "/" + path;
        }
        return "/";
    }
}