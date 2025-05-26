package io.github.t1willi.openapi.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.t1willi.annotations.Controller;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.annotations.Post;
import io.github.t1willi.openapi.annotations.Docs;
import io.github.t1willi.openapi.annotations.OpenApi;
import lombok.Getter;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@Getter
public final class OpenApiModel {
    private String openapi = "3.0.3";
    private InfoModel info;
    private Map<String, PathItemModel> paths;
    private ComponentsModel components;

    public static OpenApiModel of(OpenApi openApi, List<Map.Entry<Docs, Method>> docs) {
        ObjectMapper mapper = new ObjectMapper()
                .setSerializationInclusion(Include.NON_NULL);

        OpenApiModel model = new OpenApiModel();
        model.info = InfoModel.of(openApi);
        model.paths = new LinkedHashMap<>();
        model.components = new ComponentsModel();

        Set<Type> refs = new LinkedHashSet<>();

        for (var entry : docs) {
            Docs doc = entry.getKey();
            Method m = entry.getValue();
            String path = getPath(m, doc);

            PathItemModel item = model.paths
                    .computeIfAbsent(path, k -> new PathItemModel());

            OperationModel op = OperationModel.of(doc, m, mapper);
            item.addOperation(m, op);

            if (op.getRequestBody() != null) {
                Type reqType = op.getRequestBody().getSchemaType();
                if (reqType != null) {
                    Class<?> rawReq = mapper.getTypeFactory().constructType(reqType).getRawClass();
                    if (!Void.class.equals(rawReq)) {
                        SchemaModel sm = SchemaModel.of(reqType, mapper);
                        if (sm != null && sm.getRef() != null) {
                            refs.add(reqType);
                        }
                    }
                }
            }

            op.getResponses().values().forEach(rm -> {
                Type respType = rm.getContentType();
                if (respType != null) {
                    Class<?> rawResp = mapper.getTypeFactory().constructType(respType).getRawClass();
                    if (!Void.class.equals(rawResp)) {
                        SchemaModel sm = SchemaModel.of(respType, mapper);
                        if (sm != null && sm.getRef() != null) {
                            refs.add(respType);
                        }
                    }
                }
            });
        }

        Map<String, SchemaModel> defs = new LinkedHashMap<>();
        for (Type t : refs) {
            Class<?> raw = mapper.getTypeFactory().constructType(t).getRawClass();
            String name = raw.getSimpleName();
            SchemaModel def = SchemaModel.buildDefinition(t, mapper);
            defs.put(name, def);
        }
        model.components.setSchemas(defs);
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