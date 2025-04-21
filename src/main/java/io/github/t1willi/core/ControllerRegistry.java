package io.github.t1willi.core;

import io.github.t1willi.annotations.Controller;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.annotations.Mapping;
import io.github.t1willi.annotations.Post;
import io.github.t1willi.annotations.Put;
import io.github.t1willi.annotations.Delete;
import io.github.t1willi.annotations.Path;
import io.github.t1willi.annotations.Query;
import io.github.t1willi.exceptions.JoltDIException;
import io.github.t1willi.exceptions.JoltRoutingException;
import io.github.t1willi.form.Form;
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
        Class<?> cls = controller.getClass();
        Controller ann = cls.getAnnotation(Controller.class);
        String raw = ann.value();
        String basePath = normalize(raw.isBlank() ? "/" : resolveBasePath(cls, raw));

        JoltContainer.getInstance().inject(controller);
        Router router = Optional.ofNullable(JoltContainer.getInstance().getBean(Router.class))
                .orElseThrow(() -> new JoltDIException("Router bean not found"));

        router.before(ctx -> ((BaseController) controller).before(ctx), basePath);
        router.after(ctx -> ((BaseController) controller).after(ctx), basePath);
        ((BaseController) controller).getSpecificBeforeHandlers()
                .forEach(h -> router.before(h.handler(), h.paths().stream()
                        .map(p -> normalize(basePath + p)).toArray(String[]::new)));
        ((BaseController) controller).getSpecificAfterHandlers()
                .forEach(h -> router.after(h.handler(), h.paths().stream()
                        .map(p -> normalize(basePath + p)).toArray(String[]::new)));

        for (Method method : cls.getDeclaredMethods()) {
            validateSignature(method);
            for (var entry : List.of(
                    Map.entry(Get.class, HttpMethod.GET),
                    Map.entry(Post.class, HttpMethod.POST),
                    Map.entry(Put.class, HttpMethod.PUT),
                    Map.entry(Delete.class, HttpMethod.DELETE))) {

                if (method.isAnnotationPresent(entry.getKey())) {
                    String p = invokeValue(method.getAnnotation(entry.getKey()));
                    route(router, controller, method, entry.getValue(), normalize(basePath + p));
                }
            }
            if (method.isAnnotationPresent(Mapping.class)) {
                Mapping m = method.getAnnotation(Mapping.class);
                route(router, controller, method, m.method(), normalize(basePath + m.value()));
            }
        }
    }

    private static String resolveBasePath(Class<?> cls, String rawValue) {
        if (rawValue.contains("[controller]")) {
            String simple = cls.getSimpleName();
            String name = simple.endsWith("Controller")
                    ? simple.substring(0, simple.length() - "Controller".length())
                    : simple;
            String segment = name.toLowerCase();
            return rawValue.replace("[controller]", segment);
        }
        return rawValue;
    }

    private static void route(Router router, Object ctrl, Method method, HttpMethod verb, String path) {
        router.route(verb, path, createHandler(ctrl, method));
    }

    private static void validateSignature(Method m) {
        int mods = m.getModifiers();
        if (!Modifier.isPublic(mods) || Modifier.isStatic(mods))
            throw new JoltDIException(m.getName() + " must be public, non-static");
    }

    private static RouteHandler createHandler(Object ctrl, Method m) {
        return ctx -> {
            try {
                Object[] args = Arrays.stream(m.getParameters())
                        .map(p -> resolveParam(p, ctx, m))
                        .toArray();
                Object result = m.invoke(ctrl, args);
                return dispatchReturn(ctx, result);
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                if (cause instanceof RuntimeException re)
                    throw re;
                throw new JoltRoutingException("Error invoking " + m.getName(), cause);
            } catch (Exception e) {
                throw new JoltDIException(e.getMessage(), e);
            }
        };
    }

    private static Object resolveParam(Parameter p, JoltContext ctx, Method m) {
        if (p.isAnnotationPresent(Path.class)) {
            String raw = ctx.path(p.getAnnotation(Path.class).value());
            return HelpMethods.convert(raw, p.getType());
        }
        if (p.isAnnotationPresent(Query.class)) {
            String raw = ctx.query(p.getAnnotation(Query.class).value());
            return HelpMethods.convert(raw, p.getType());
        }
        if (JoltContext.class.isAssignableFrom(p.getType())) {
            return ctx;
        }
        if (Form.class.isAssignableFrom(p.getType())) {
            return ctx.buildForm();
        }
        if (Template.class.isAssignableFrom(p.getType())) {
            throw new JoltDIException(
                    "Template cannot be used as parameter type in controller method, for method: " + m.getName()
                            + "(...)");
        }
        try {
            return ctx.body(p.getType());
        } catch (Exception e) {
            throw new JoltDIException(
                    "Unable to bind @Body to " + p.getType().getSimpleName() + " for method: " + m.getName(), e);
        }
    }

    private static JoltContext dispatchReturn(JoltContext ctx, Object result) {
        if (result == null)
            return ctx;
        if (result instanceof JoltContext jc)
            return jc;
        if (result instanceof String s) {
            if (s.matches(".+\\.[a-z]{2,4}"))
                return ctx.serve(s);
            return ctx.html(s);
        }
        if (result instanceof Template t)
            return ctx.render(t.getView(), t.getModel());
        return ctx.json(result);
    }

    private static String invokeValue(Annotation a) {
        try {
            return (String) a.annotationType().getMethod("value").invoke(a);
        } catch (Exception e) {
            throw new JoltDIException("Failed to read @… value", e);
        }
    }

    private static String normalize(String p) {
        String r = p.replaceAll("//+", "/");
        if (!r.startsWith("/"))
            r = "/" + r;
        if (r.endsWith("/") && r.length() > 1)
            r = r.substring(0, r.length() - 1);
        return r;
    }
}