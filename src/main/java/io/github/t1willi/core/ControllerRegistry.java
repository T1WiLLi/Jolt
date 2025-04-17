package io.github.t1willi.core;

import io.github.t1willi.annotations.Delete;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.annotations.Mapping;
import io.github.t1willi.annotations.Post;
import io.github.t1willi.annotations.Put;
import io.github.t1willi.annotations.Root;
import io.github.t1willi.exceptions.JoltDIException;
import io.github.t1willi.exceptions.JoltRoutingException;
import io.github.t1willi.http.HttpMethod;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.routing.RouteHandler;
import io.github.t1willi.routing.context.JoltContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages registration of controllers in the Jolt framework.
 * Retrieves classes extending BaseController, processes their
 * routing annotations, and registers routes with the Router.
 * Enforces strict method signatures for routing methods and
 * registers controller-specific lifecycle handlers, both general
 * (for all routes) and specific (for designated routes).
 */
public final class ControllerRegistry {

    private static final Logger logger = Logger.getLogger(ControllerRegistry.class.getName());

    private ControllerRegistry() {
        // Prevent instantiation
    }

    /**
     * Scans for controllers (classes extending BaseController) and registers their
     * routes
     * and lifecycle handlers. Validates that annotated methods have the correct
     * signature.
     *
     * @throws JoltDIException if controller registration fails or method signatures
     *                         are invalid.
     */
    public static void registerControllers() {
        List<BaseController> controllers;
        try {
            controllers = JoltContainer.getInstance().getBeans(BaseController.class);
        } catch (Exception e) {
            throw new JoltDIException(
                    "Failed to retrieve BaseController implementations from JoltContainer: " + e.getMessage(), e);
        }

        if (controllers.isEmpty()) {
            logger.warning("No controllers found extending BaseController.");
            return;
        }

        for (BaseController controller : controllers) {
            registerController(controller);
        }
    }

    private static void registerController(BaseController controller) {
        try {
            if (controller == null) {
                throw new JoltDIException("Controller instance cannot be null");
            }
            JoltContainer.getInstance().inject(controller);
            Class<?> controllerClass = controller.getClass();
            String rootPath = getRootPath(controllerClass);
            registerLifecycleHandlers(controller, rootPath);
            for (Method method : controllerClass.getDeclaredMethods()) {
                registerRoutesForMethod(controller, method, rootPath);
            }
        } catch (Exception e) {
            String message = "Failed to register controller "
                    + (controller != null ? controller.getClass().getName() : "null") + ": " + e.getMessage();
            logger.severe(() -> message);
            throw new JoltDIException(message, e);
        }
    }

    private static void registerLifecycleHandlers(BaseController controller, String rootPath) {
        Router router;
        try {
            router = JoltContainer.getInstance().getBean(Router.class);
            if (router == null) {
                throw new JoltDIException("Router bean not found in JoltContainer");
            }
        } catch (Exception e) {
            throw new JoltDIException("Failed to retrieve Router bean: " + e.getMessage(), e);
        }

        router.before(controller::before, rootPath);
        router.after(controller::after, rootPath);

        for (BaseController.LifecycleHandler handlerEntry : controller.getSpecificBeforeHandlers()) {
            String[] fullPaths = handlerEntry.paths().stream()
                    .map(path -> normalizePath(rootPath + (path.isEmpty() ? "" : path)))
                    .toArray(String[]::new);
            router.before(handlerEntry.handler(), fullPaths);
        }

        for (BaseController.LifecycleHandler handlerEntry : controller.getSpecificAfterHandlers()) {
            String[] fullPaths = handlerEntry.paths().stream()
                    .map(path -> normalizePath(rootPath + (path.isEmpty() ? "" : path)))
                    .toArray(String[]::new);
            router.after(handlerEntry.handler(), fullPaths);
        }
    }

    private static String getRootPath(Class<?> controllerClass) {
        if (controllerClass == null) {
            throw new JoltDIException("Controller class cannot be null");
        }

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

        processAnnotation(method, controller, Get.class, HttpMethod.GET, routes);
        processAnnotation(method, controller, Post.class, HttpMethod.POST, routes);
        processAnnotation(method, controller, Put.class, HttpMethod.PUT, routes);
        processAnnotation(method, controller, Delete.class, HttpMethod.DELETE, routes);

        if (method.isAnnotationPresent(Mapping.class)) {
            validateMethodSignature(method, controller.getClass());
            Mapping mapping = method.getAnnotation(Mapping.class);
            routes.add(new RouteDefinition(mapping.method(), mapping.value()));
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

    private static void processAnnotation(Method method, BaseController controller,
            Class<? extends Annotation> annotationClass, HttpMethod httpMethod, List<RouteDefinition> routes) {
        if (method.isAnnotationPresent(annotationClass)) {
            validateMethodSignature(method, controller.getClass());
            Annotation annotation = method.getAnnotation(annotationClass);
            String path = extractPathFromAnnotation(annotation);
            routes.add(new RouteDefinition(httpMethod, path));
        }
    }

    private static String extractPathFromAnnotation(Object annotation) {
        try {
            Method pathMethod = annotation.getClass().getMethod("value");
            return (String) pathMethod.invoke(annotation);
        } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new JoltDIException("Failed to extract path from annotation: " + e.getMessage(), e);
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
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                if (cause instanceof RuntimeException re) {
                    throw re;
                }
                throw new JoltRoutingException(
                        "Error invoking " + method.getName() +
                                " in " + controller.getClass().getSimpleName() + ": " +
                                cause.getMessage(),
                        cause);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                throw new JoltDIException(
                        "Cannot invoke " + method.getName() +
                                " in " + controller.getClass().getSimpleName() +
                                ": " + e.getMessage(),
                        e);
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