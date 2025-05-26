package io.github.t1willi.openapi.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.t1willi.openapi.annotations.Docs;
import lombok.Getter;
import lombok.Setter;
import io.github.t1willi.annotations.*;
import java.lang.reflect.Method;

@Setter
@Getter
public final class PathItemModel {
    private OperationModel get;
    private OperationModel put;
    private OperationModel post;
    private OperationModel delete;
    private OperationModel patch;

    public static PathItemModel of(Docs docs, Method method, ObjectMapper mapper) {
        PathItemModel model = new PathItemModel();
        OperationModel operation = OperationModel.of(docs, mapper);

        if (method != null) {
            if (method.isAnnotationPresent(Get.class)) {
                model.setGet(operation);
            } else if (method.isAnnotationPresent(Post.class)) {
                model.setPost(operation);
            } else if (method.isAnnotationPresent(Put.class)) {
                model.setPut(operation);
            } else if (method.isAnnotationPresent(Delete.class)) {
                model.setDelete(operation);
            } else if (method.isAnnotationPresent(Mapping.class)) {
                Mapping mapping = method.getAnnotation(Mapping.class);
                switch (mapping.method()) {
                    case GET:
                        model.setGet(operation);
                        break;
                    case POST:
                        model.setPost(operation);
                        break;
                    case PUT:
                        model.setPut(operation);
                        break;
                    case DELETE:
                        model.setDelete(operation);
                        break;
                    case PATCH:
                        model.setPatch(operation);
                        break;
                    default:
                        model.setGet(operation);
                }
            } else {
                String methodName = method.getName().toLowerCase();
                if (methodName.startsWith("get")) {
                    model.setGet(operation);
                } else if (methodName.startsWith("post")) {
                    model.setPost(operation);
                } else if (methodName.startsWith("put")) {
                    model.setPut(operation);
                } else if (methodName.startsWith("delete")) {
                    model.setDelete(operation);
                } else if (methodName.startsWith("patch")) {
                    model.setPatch(operation);
                } else {
                    model.setGet(operation);
                }
            }
        } else {
            model.setGet(operation);
        }
        return model;
    }
}