package io.github.t1willi.core;

import io.github.t1willi.annotations.*;
import io.github.t1willi.exceptions.JoltDIException;
import io.github.t1willi.exceptions.JoltRoutingException;
import io.github.t1willi.http.HttpMethod;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.routing.RouteHandler;
import io.github.t1willi.routing.context.JoltContext;
import io.github.t1willi.template.Template;
import io.github.t1willi.utils.HelpMethods;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Scans all @Controller beans, registers their @Get/@Post/etc. methods
 * (and any @Mapping), injects path/query/body/form parameters,
 * and dispatches return types (JoltContext, String, POJO, Template).
 */
public final class ControllerRegistry {
    private static final Logger log = Logger.getLogger(ControllerRegistry.class.getName());

    private ControllerRegistry() {
    }

    public static void registerControllers() {
        List<Object> controllers = Optional.ofNullable(
                JoltContainer.getInstance().getBeans(Object.class))
                .orElse(List.of()).stream()
                .filter(bean -> bean.getClass().isAnnotationPresent(Controller.class))
                .toList();

        if (controllers.isEmpty()) {
            log.warning("No @Controller beans found.");
            return;
        }
        for (Object controller : controllers) {
            registerController(controller);
        }
    }

    private static void registerController(Object controller) {
        Class<?> controllerClass = controller.getClass();
        Controller controllerAnnotation = controllerClass.getAnnotation(Controller.class);
        String basePath = normalizePath(controllerAnnotation.value().isBlank()
                ? "/"
                : controllerAnnotation.value());

        JoltContainer.getInstance().inject(controller);
        Router router = Optional.ofNullable(JoltContainer.getInstance().getBean(Router.class))
                .orElseThrow(() -> new JoltDIException("Router bean not found"));

        router.before(ctx -> ((BaseController) controller).before(ctx), basePath);
        router.after(ctx -> ((BaseController) controller).after(ctx), basePath);

        for (var entry : ((BaseController) controller).getSpecificBeforeHandlers()) {
            router.before(entry.handler(),
                    entry.paths().stream()
                            .map(p -> normalizePath(basePath + p))
                            .toArray(String[]::new));
        }
        for (var entry : ((BaseController) controller).getSpecificAfterHandlers()) {
            router.after(entry.handler(),
                    entry.paths().stream()
                            .map(p -> normalizePath(basePath + p))
                            .toArray(String[]::new));
        }

        for (Method method : controllerClass.getDeclaredMethods()) {
            List<RouteDefinition> routes = collectRouteDefinitions(method);
            for (RouteDefinition route : routes) {
                String fullPath = normalizePath(basePath + route.path());
                router.route(route.method(), fullPath, createRouteHandler(controller, method));
            }
        }
    }

    private static List<RouteDefinition> collectRouteDefinitions(Method method) {
        validateMethodSignature(method);

        List<RouteDefinition> definitions = new ArrayList<>();

        List<Map.Entry<Class<? extends Annotation>, HttpMethod>> mappings = List.of(
                Map.entry(Get.class, HttpMethod.GET),
                Map.entry(Post.class, HttpMethod.POST),
                Map.entry(Put.class, HttpMethod.PUT),
                Map.entry(Delete.class, HttpMethod.DELETE));

        for (var mapEntry : mappings) {
            Class<? extends Annotation> annotationType = mapEntry.getKey();
            HttpMethod httpMethod = mapEntry.getValue();
            if (method.isAnnotationPresent(annotationType)) {
                Annotation annotation = method.getAnnotation(annotationType);
                String pathValue = invokeValueMethod(annotation);
                definitions.add(new RouteDefinition(httpMethod, pathValue));
            }
        }

        if (method.isAnnotationPresent(Mapping.class)) {
            Mapping mapping = method.getAnnotation(Mapping.class);
            definitions.add(new RouteDefinition(mapping.method(), mapping.value()));
        }

        return definitions;
    }

    private static void validateMethodSignature(Method method) {
        int mods = method.getModifiers();
        if (!Modifier.isPublic(mods))
            throw new JoltDIException(method.getName() + " must be public");
        if (Modifier.isStatic(mods))
            throw new JoltDIException(method.getName() + " must not be static");
    }

    private static RouteHandler createRouteHandler(Object controller, Method method) {
        return ctx -> {
            try {
                Object[] args = Arrays.stream(method.getParameters())
                        .map(param -> resolveParameter(param, ctx))
                        .toArray();
                Object result = method.invoke(controller, args);
                return handleReturn(ctx, result);
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                if (cause instanceof RuntimeException re)
                    throw re;
                throw new JoltRoutingException("Error invoking " + method.getName(), cause);
            } catch (Exception e) {
                throw new JoltDIException("Cannot invoke " + method.getName(), e);
            }
        };
    }

    private static Object resolveParameter(Parameter parameter, JoltContext ctx) {
        if (parameter.isAnnotationPresent(RequestPath.class)) {
            String raw = ctx.path(parameter.getAnnotation(RequestPath.class).value());
            return HelpMethods.convert(raw, parameter.getType());
        }
        if (parameter.isAnnotationPresent(RequestQuery.class)) {
            String raw = ctx.query(parameter.getAnnotation(RequestQuery.class).value());
            return HelpMethods.convert(raw, parameter.getType());
        }
        if (parameter.isAnnotationPresent(RequestBody.class)) {
            return ctx.body(parameter.getType());
        }
        if (parameter.isAnnotationPresent(RequestForm.class)) {
            return ctx.buildForm();
        }
        if (JoltContext.class.isAssignableFrom(parameter.getType())) {
            return ctx;
        }
        throw new JoltDIException("Unsupported parameter: " + parameter.getName());
    }

    private static JoltContext handleReturn(JoltContext ctx, Object result) {
        if (result == null) {
            return ctx;
        }
        if (result instanceof JoltContext jc) {
            return jc;
        }
        if (result instanceof String html) {
            return ctx.html(html);
        }
        if (result instanceof Template tpl) {
            return ctx.render(tpl.getView(), tpl.getModel());
        }
        return ctx.json(result);
    }

    private static String invokeValueMethod(Annotation annotation) {
        try {
            return (String) annotation.annotationType()
                    .getMethod("value")
                    .invoke(annotation);
        } catch (Exception e) {
            throw new JoltDIException("Failed to read path from " + annotation, e);
        }
    }

    private static String normalizePath(String path) {
        String p = path.replaceAll("//+", "/");
        if (!p.startsWith("/"))
            p = "/" + p;
        if (p.endsWith("/") && p.length() > 1)
            p = p.substring(0, p.length() - 1);
        return p;
    }

    private record RouteDefinition(HttpMethod method, String path) {
    }
}
