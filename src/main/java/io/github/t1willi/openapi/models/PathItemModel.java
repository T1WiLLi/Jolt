package io.github.t1willi.openapi.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.t1willi.annotations.Delete;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.annotations.Mapping;
import io.github.t1willi.annotations.Post;
import io.github.t1willi.annotations.Put;
import io.github.t1willi.openapi.annotations.Docs;
import lombok.Getter;
import lombok.Setter;

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
        OperationModel operation = OperationModel.of(docs, method, mapper);

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
                    case GET -> model.setGet(operation);
                    case POST -> model.setPost(operation);
                    case PUT -> model.setPut(operation);
                    case DELETE -> model.setDelete(operation);
                    case PATCH -> model.setPatch(operation);
                    case HEAD, OPTIONS, TRACE -> {
                        // No OperationModel field for these methods, so do nothing or add handling if
                        // needed
                    }
                }
            }
        }
        return model;
    }
}