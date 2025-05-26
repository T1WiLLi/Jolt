package io.github.t1willi.openapi.models;

import io.github.t1willi.annotations.Delete;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.annotations.Mapping;
import io.github.t1willi.annotations.Post;
import io.github.t1willi.annotations.Put;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Method;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@Setter
@Getter
public final class PathItemModel {
    private OperationModel get;
    private OperationModel put;
    private OperationModel post;
    private OperationModel delete;
    private OperationModel patch;

    public void addOperation(Method method, OperationModel operation) {
        if (method.isAnnotationPresent(Get.class)) {
            setGet(operation);
        } else if (method.isAnnotationPresent(Post.class)) {
            setPost(operation);
        } else if (method.isAnnotationPresent(Put.class)) {
            setPut(operation);
        } else if (method.isAnnotationPresent(Delete.class)) {
            setDelete(operation);
        } else if (method.isAnnotationPresent(Mapping.class)) {
            switch (method.getAnnotation(Mapping.class).method()) {
                case GET -> {
                    setGet(operation);
                }
                case POST -> {
                    setPost(operation);
                }
                case PUT -> {
                    setPut(operation);
                }
                case DELETE -> {
                    setDelete(operation);
                }
                case PATCH -> {
                    setPatch(operation);
                }
                case HEAD, OPTIONS, TRACE -> {
                    // No operation
                }
            }
        }
    }
}