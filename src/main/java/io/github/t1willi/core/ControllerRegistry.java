package io.github.t1willi.core;

import io.github.t1willi.annotations.Controller;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.annotations.Mapping;
import io.github.t1willi.annotations.Post;
import io.github.t1willi.annotations.Put;
import io.github.t1willi.annotations.Delete;
import io.github.t1willi.annotations.Path;
import io.github.t1willi.annotations.Query;
import io.github.t1willi.context.JoltContext;
import io.github.t1willi.exceptions.JoltDIException;
import io.github.t1willi.exceptions.JoltRoutingException;
import io.github.t1willi.form.Form;
import io.github.t1willi.http.HttpMethod;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.routing.RouteHandler;
import io.github.t1willi.security.authentification.AuthStrategy;
import io.github.t1willi.security.authentification.Authorize;
import io.github.t1willi.security.authentification.RouteRule;
import io.github.t1willi.template.Template;
import io.github.t1willi.utils.HelpMethods;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

public final class ControllerRegistry {
    private static final Logger log = Logger.getLogger(ControllerRegistry.class.getName());

    public static final List<RouteRule> AUTHORIZATION = new ArrayList<>();

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
        String basePath = computeHierarchicalBasePath(cls);

        JoltContainer.getInstance().inject(controller);

        Router router = Optional.ofNullable(JoltContainer.getInstance().getBean(Router.class))
                .orElseThrow(() -> new JoltDIException("Router bean not found"));

        registerBeforeAfterHandlers(controller, router, basePath);

        Authorize classAuth = cls.getAnnotation(Authorize.class);

        for (Method method : cls.getDeclaredMethods()) {
            validateSignature(method);
            processHttpMethodAnnotations(controller, method, router, basePath, classAuth);
            processMappingAnnotation(controller, method, router, basePath, classAuth);
        }
    }

    private static void registerBeforeAfterHandlers(Object controller, Router router, String basePath) {
        BaseController baseController = (BaseController) controller;

        router.before(ctx -> baseController.before(ctx), basePath);
        router.after(ctx -> baseController.after(ctx), basePath);

        baseController.getSpecificBeforeHandlers()
                .forEach(h -> router.before(
                        h.handler(),
                        h.paths().stream()
                                .map(p -> normalize(basePath + p))
                                .toArray(String[]::new)));
        baseController.getSpecificAfterHandlers()
                .forEach(h -> router.after(
                        h.handler(),
                        h.paths().stream()
                                .map(p -> normalize(basePath + p))
                                .toArray(String[]::new)));
    }

    private static void processHttpMethodAnnotations(
            Object controller,
            Method method,
            Router router,
            String basePath,
            Authorize classAuth) {

        List<Map.Entry<Class<? extends Annotation>, HttpMethod>> httpMethodAnnotations = List.of(
                Map.entry(Get.class, HttpMethod.GET),
                Map.entry(Post.class, HttpMethod.POST),
                Map.entry(Put.class, HttpMethod.PUT),
                Map.entry(Delete.class, HttpMethod.DELETE));

        for (var entry : httpMethodAnnotations) {
            Class<? extends Annotation> annotationType = entry.getKey();

            if (!method.isAnnotationPresent(annotationType)) {
                continue;
            }

            String rawPath = invokeValue(method.getAnnotation(annotationType));
            String fullPath = normalize(basePath + rawPath);
            HttpMethod verb = entry.getValue();

            RouteHandler handler = createHandler(controller, method);
            router.route(verb, fullPath, handler);
            processAuthorization(method, classAuth, fullPath, verb);
        }
    }

    private static void processMappingAnnotation(
            Object controller,
            Method method,
            Router router,
            String basePath,
            Authorize classAuth) {

        if (!method.isAnnotationPresent(Mapping.class)) {
            return;
        }

        Mapping mapping = method.getAnnotation(Mapping.class);
        String fullPath = normalize(basePath + mapping.value());
        HttpMethod verb = mapping.method();

        RouteHandler handler = createHandler(controller, method);
        router.route(verb, fullPath, handler);

        processAuthorizationWithContainer(method, classAuth, fullPath, verb);
    }

    private static void processAuthorization(Method method, Authorize classAuth, String fullPath, HttpMethod verb) {
        Authorize methodAuth = method.getAnnotation(Authorize.class);
        Authorize effective = methodAuth != null ? methodAuth : classAuth;

        if (effective == null) {
            return;
        }

        try {
            AuthStrategy strategy = effective.value().getDeclaredConstructor().newInstance();
            AUTHORIZATION.add(new RouteRule(
                    fullPath,
                    false,
                    Set.of(verb.name()),
                    strategy,
                    false,
                    false));
        } catch (Exception e) {
            throw new JoltDIException("Failed to create AuthStrategy for method: " + method.getName(), e);
        }
    }

    private static void processAuthorizationWithContainer(Method method, Authorize classAuth, String fullPath,
            HttpMethod verb) {
        Authorize methodAuth = method.getAnnotation(Authorize.class);
        Authorize effective = methodAuth != null ? methodAuth : classAuth;

        if (effective == null) {
            return;
        }

        try {
            AuthStrategy strategy = JoltContainer.getInstance().getBean(effective.value());
            if (strategy != null) {
                AUTHORIZATION.add(new RouteRule(
                        fullPath,
                        false,
                        Set.of(verb.name()),
                        strategy,
                        false,
                        false));
            }
        } catch (Exception e) {
            throw new JoltDIException("Failed to resolve AuthStrategy for method: " + method.getName(), e);
        }
    }

    private static String computeHierarchicalBasePath(Class<?> cls) {
        Class<?> parent = cls.getSuperclass();
        String parentPath = "";
        if (parent != null && parent.isAnnotationPresent(Controller.class)) {
            parentPath = computeHierarchicalBasePath(parent);
        }
        Controller ann = cls.getAnnotation(Controller.class);
        if (ann == null) {
            return normalize(parentPath);
        }
        String raw = ann.value();
        String segment = raw.isBlank() ? "" : resolveBasePathSegment(cls, raw);
        return normalize(parentPath + "/" + segment);
    }

    private static String resolveBasePathSegment(Class<?> cls, String rawValue) {
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