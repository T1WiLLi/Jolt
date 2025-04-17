package io.github.t1willi.core;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import io.github.t1willi.annotations.Delete;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.annotations.Mapping;
import io.github.t1willi.annotations.Post;
import io.github.t1willi.annotations.Put;
import io.github.t1willi.annotations.Root;
import io.github.t1willi.exceptions.JoltDIException;
import io.github.t1willi.http.HttpMethod;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.routing.RouteHandler;
import io.github.t1willi.routing.context.JoltContext;

public final class ControllerRegistry {
    private static final Logger logger = Logger.getLogger(ControllerRegistry.class.getName());

    public static void registerControllers() {
        List<BaseController> controllers = JoltContainer.getInstance().getBeans(BaseController.class);
        for (BaseController controller : controllers) {
            registerController(controller);
        }
    }

    private static void registerController(BaseController controller) {
        try {
            JoltContainer.getInstance().inject(controller);
            Class<?> controllerClass = controller.getClass();

            String rootPath = getRootPath(controllerClass);

            for (Method method : controllerClass.getMethods()) {
                registerRoutesForMethod(controller, method, rootPath);
            }
        } catch (Exception e) {
            logger.severe(
                    () -> "Failed to register controller " + controller.getClass().getName() + ": " + e.getMessage());
            throw new JoltDIException("Controller registration failed", e);
        }
    }

    private static String getRootPath(Class<?> controllerClass) {
        Root root = controllerClass.getAnnotation(Root.class);
        String rootValue = (root != null) ? root.value() : "";
        if (rootValue.equals("[controller]")) {
            String className = controllerClass.getSimpleName();
            if (className.endsWith("Controller")) {
                className = className.substring(0, className.length() - "Controller".length());
            }
            return "/" + className.toLowerCase();
        }
        return rootValue.isEmpty() ? "/" : rootValue;
    }

    private static void registerRoutesForMethod(BaseController controller, Method method, String rootPath) {
        List<RouteDefinition> routes = new ArrayList<>();

        // Check for method-level annotations
        if (method.isAnnotationPresent(Get.class)) {
            validateMethodSignature(method, controller.getClass());
            Get get = method.getAnnotation(Get.class);
            routes.add(new RouteDefinition(HttpMethod.GET, get.path()));
        }
        if (method.isAnnotationPresent(Post.class)) {
            validateMethodSignature(method, controller.getClass());
            Post post = method.getAnnotation(Post.class);
            routes.add(new RouteDefinition(HttpMethod.POST, post.path()));
        }
        if (method.isAnnotationPresent(Put.class)) {
            validateMethodSignature(method, controller.getClass());
            Put put = method.getAnnotation(Put.class);
            routes.add(new RouteDefinition(HttpMethod.PUT, put.path()));
        }
        if (method.isAnnotationPresent(Delete.class)) {
            validateMethodSignature(method, controller.getClass());
            Delete delete = method.getAnnotation(Delete.class);
            routes.add(new RouteDefinition(HttpMethod.DELETE, delete.path()));
        }
        if (method.isAnnotationPresent(Mapping.class)) {
            validateMethodSignature(method, controller.getClass());
            Mapping mapping = method.getAnnotation(Mapping.class);
            routes.add(new RouteDefinition(mapping.method(), mapping.path()));
        }

        for (RouteDefinition route : routes) {
            String fullPath = normalizePath(rootPath + route.path);
            RouteHandler handler = createRouteHandler(controller, method);
            try {
                Router router = JoltContainer.getInstance().getBean(Router.class);
                if (router == null) {
                    throw new JoltDIException("Router bean not found in JoltContainer");
                }
                router.route(route.method, fullPath, handler);
            } catch (Exception e) {
                throw new JoltDIException(
                        "Failed to register route " + route.method + " " + fullPath + " for method " + method.getName()
                                + " in controller " + controller.getClass().getName() + ": " + e.getMessage(),
                        e);
            }
        }
    }

    private static void validateMethodSignature(Method method, Class<?> controllerClass) {
        String methodDetails = "Method " + method.getName() + " in controller " + controllerClass.getName();

        if (!Modifier.isPublic(method.getModifiers())) {
            throw new JoltDIException(methodDetails + " must be public to be used as a route handler");
        }

        if (Modifier.isStatic(method.getModifiers())) {
            throw new JoltDIException(methodDetails + " must not be static to be used as a route handler");
        }

        if (!JoltContext.class.isAssignableFrom(method.getReturnType())) {
            throw new JoltDIException(
                    methodDetails + " must return JoltContext to be used as a route handler; found return type: "
                            + method.getReturnType().getName());
        }

        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            throw new JoltDIException(methodDetails + " must have exactly one parameter of type JoltContext; found "
                    + parameterTypes.length + " parameters");
        }
        if (!JoltContext.class.isAssignableFrom(parameterTypes[0])) {
            throw new JoltDIException(
                    methodDetails + " must have a single parameter of type JoltContext; found parameter type: "
                            + parameterTypes[0].getName());
        }
    }

    private static RouteHandler createRouteHandler(BaseController controller, Method method) {
        return ctx -> {
            try {
                return (JoltContext) method.invoke(controller, ctx);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke controller method " + method.getName(), e);
            }
        };
    }

    private static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        String normalized = path.replaceAll("//+", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static record RouteDefinition(HttpMethod method, String path) {
    }
}