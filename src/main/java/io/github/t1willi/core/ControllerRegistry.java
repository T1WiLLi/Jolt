package io.github.t1willi.core;

import io.github.t1willi.annotations.*;
import io.github.t1willi.context.JoltContext;
import io.github.t1willi.exceptions.JoltDIException;
import io.github.t1willi.exceptions.JoltRoutingException;
import io.github.t1willi.form.Form;
import io.github.t1willi.http.HttpMethod;
import io.github.t1willi.http.ModelView;
import io.github.t1willi.http.ResponseEntity;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.routing.RouteHandler;
import io.github.t1willi.security.authentification.AuthStrategy;
import io.github.t1willi.security.authentification.Authorize;
import io.github.t1willi.security.authentification.AuthorizationCredentials;
import io.github.t1willi.security.authentification.RouteRule;
import io.github.t1willi.template.JoltModel;
import io.github.t1willi.utils.HelpMethods;
import lombok.Getter;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.logging.Logger;

public final class ControllerRegistry {
    private static final Logger log = Logger.getLogger(ControllerRegistry.class.getName());
    public static final List<RouteRule> AUTHORIZATION = new ArrayList<>();
    @Getter
    private static List<BaseController> controllers = new ArrayList<>();

    private ControllerRegistry() {
    }

    public static void registerControllers() {
        List<BaseController> controllers = fetchControllers();
        ControllerRegistry.controllers = controllers;
        controllers.forEach(ControllerRegistry::registerController);
    }

    private static List<BaseController> fetchControllers() {
        try {
            return JoltContainer.getInstance().getBeans(BaseController.class).stream()
                    .filter(bean -> bean.getClass().isAnnotationPresent(Controller.class))
                    .toList();
        } catch (Exception e) {
            log.warning("No @Controller beans found.");
            return List.of();
        }
    }

    private static void registerController(BaseController controller) {
        Class<?> cls = controller.getClass();
        String basePath = computeHierarchicalBasePath(cls);
        Version classVerAnn = cls.getAnnotation(Version.class);
        int classVersion = classVerAnn != null ? classVerAnn.value() : 0;
        String classPrefix = classVerAnn != null ? classVerAnn.prefix() : "v";
        Authorize classAuth = cls.getAnnotation(Authorize.class);

        JoltContainer.getInstance().inject(controller);
        Router router = JoltContainer.getInstance().getBean(Router.class);
        if (router == null) {
            throw new JoltDIException("Router bean not found");
        }

        registerLifecycles(controller, router, basePath);
        filterMethods(cls.getMethods()).forEach(
                method -> registerRoutes(controller, method, router, basePath, classAuth, classVersion, classPrefix));
    }

    private static List<Method> filterMethods(Method[] methods) {
        return Arrays.stream(methods)
                .filter(m -> m.isAnnotationPresent(Get.class) || m.isAnnotationPresent(Post.class) ||
                        m.isAnnotationPresent(Put.class) || m.isAnnotationPresent(Delete.class) ||
                        m.isAnnotationPresent(Mapping.class))
                .toList();
    }

    private static void registerRoutes(Object controller, Method method, Router router, String basePath,
            Authorize classAuth, int classVersion, String classPrefix) {
        validateSignature(method);
        registerHttpRoutes(controller, method, router, basePath, classAuth, classVersion, classPrefix);
        registerMappingRoute(controller, method, router, basePath, classAuth, classVersion, classPrefix);
    }

    private static void registerLifecycles(BaseController controller, Router router, String basePath) {
        router.before(controller::before, basePath);
        router.after(controller::after, basePath);
        controller.getSpecificBeforeHandlers().forEach(h -> router.before(h.handler(),
                h.paths().stream().map(p -> normalize(basePath + p)).toArray(String[]::new)));
        controller.getSpecificAfterHandlers().forEach(h -> router.after(h.handler(),
                h.paths().stream().map(p -> normalize(basePath + p)).toArray(String[]::new)));
    }

    private static void registerHttpRoutes(Object controller, Method method, Router router, String basePath,
            Authorize classAuth, int classVersion, String classPrefix) {
        Map<Class<? extends Annotation>, HttpMethod> httpAnnotations = Map.of(
                Get.class, HttpMethod.GET, Post.class, HttpMethod.POST,
                Put.class, HttpMethod.PUT, Delete.class, HttpMethod.DELETE);
        httpAnnotations.forEach((annType, verb) -> {
            if (method.isAnnotationPresent(annType)) {
                Annotation ann = method.getAnnotation(annType);
                String path = buildPath(basePath, invokeValue(ann), getEffectiveVersion(method, classVersion),
                        classPrefix);
                router.route(verb, path, createHandler(controller, method));
                applyAuthorization(method, classAuth, path, verb);
            }
        });
    }

    private static void registerMappingRoute(Object controller, Method method, Router router, String basePath,
            Authorize classAuth, int classVersion, String classPrefix) {
        if (!method.isAnnotationPresent(Mapping.class)) {
            return;
        }
        Mapping mapping = method.getAnnotation(Mapping.class);
        String path = buildPath(basePath, mapping.value(), getEffectiveVersion(method, classVersion), classPrefix);
        router.route(mapping.method(), path, createHandler(controller, method));
        processAuthorizationWithContainer(method, classAuth, path, mapping.method());
    }

    private static int getEffectiveVersion(Method method, int classVersion) {
        Version verAnn = method.getAnnotation(Version.class);
        return verAnn != null ? verAnn.value() : classVersion;
    }

    private static void applyAuthorization(Method method, Authorize classAuth, String fullPath, HttpMethod verb) {
        Authorize effective = method.isAnnotationPresent(Authorize.class) ? method.getAnnotation(Authorize.class)
                : classAuth;
        if (effective == null) {
            return;
        }
        try {
            AuthStrategy strategy = effective.strategy().getDeclaredConstructor().newInstance();
            Map<String, Object> credentials = parseCredentials(method);
            AUTHORIZATION.add(new RouteRule(fullPath, false, Set.of(verb.name()), strategy, false, false,
                    effective.onFailure().isEmpty() ? null : effective.onFailure(), credentials));
        } catch (Exception e) {
            throw new JoltDIException("Failed to create AuthStrategy for method: " + method.getName(), e);
        }
    }

    private static void processAuthorizationWithContainer(Method method, Authorize classAuth, String fullPath,
            HttpMethod verb) {
        Authorize effective = method.isAnnotationPresent(Authorize.class) ? method.getAnnotation(Authorize.class)
                : classAuth;
        if (effective == null) {
            return;
        }
        try {
            AuthStrategy strategy = JoltContainer.getInstance().getBean(effective.strategy());
            if (strategy != null) {
                Map<String, Object> credentials = parseCredentials(method);
                AUTHORIZATION.add(new RouteRule(fullPath, false, Set.of(verb.name()), strategy, false, false,
                        effective.onFailure().isEmpty() ? null : effective.onFailure(), credentials));
            }
        } catch (Exception e) {
            throw new JoltDIException("Failed to resolve AuthStrategy for method: " + method.getName(), e);
        }
    }

    private static Map<String, Object> parseCredentials(Method method) {
        AuthorizationCredentials ann = method.getAnnotation(AuthorizationCredentials.class);
        if (ann == null) {
            Class<?> cls = method.getDeclaringClass();
            ann = cls.getAnnotation(AuthorizationCredentials.class);
        }
        if (ann == null) {
            return Map.of();
        }
        validateCredentialsAnnotation(ann);
        return buildCredentialsMap(ann);
    }

    private static void validateCredentialsAnnotation(AuthorizationCredentials ann) {
        if (ann.key().length != ann.expected().length) {
            throw new IllegalArgumentException("Mismatched key and expected arrays in @AuthorizationCredentials");
        }
        if (ann.expectedTypes().length > 0 && ann.expectedTypes().length != ann.key().length) {
            throw new IllegalArgumentException("Mismatched expectedTypes array in @AuthorizationCredentials");
        }
    }

    private static Map<String, Object> buildCredentialsMap(AuthorizationCredentials ann) {
        Map<String, Object> credentials = new HashMap<>();
        String[] keys = ann.key();
        String[] expecteds = ann.expected();
        Class<?>[] types = ann.expectedTypes();
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] == null || keys[i].isEmpty()) {
                throw new IllegalArgumentException("Null or empty key in @AuthorizationCredentials");
            }
            Class<?> type = (i < types.length) ? types[i] : null;
            credentials.put(keys[i], parseExpectedValue(keys[i], expecteds[i], type));
        }
        return credentials;
    }

    private static Object parseExpectedValue(String key, String value, Class<?> expectedType) {
        if (value == null) {
            return null;
        }
        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return "";
        }
        if (expectedType != null) {
            return parseWithType(key, trimmedValue, expectedType);
        }
        return smartParse(trimmedValue);
    }

    private static Object parseWithType(String key, String value, Class<?> type) {
        try {
            return HelpMethods.convert(value, type);
        } catch (JoltDIException e) {
            throw new IllegalArgumentException("Invalid " + type.getSimpleName() + " value for key '" + key + "'", e);
        }
    }

    private static Object smartParse(String value) {
        return HelpMethods.smartParse(value);
    }

    private static String buildPath(String base, String raw, int version, String prefix) {
        String path = normalize(base + normalizePath(raw));
        return version > 0 ? "/" + prefix + version + path : path;
    }

    private static String computeHierarchicalBasePath(Class<?> cls) {
        Deque<String> segments = new ArrayDeque<>();
        Class<?> cur = cls;
        while (cur != null && cur.isAnnotationPresent(Controller.class)) {
            Controller ann = cur.getAnnotation(Controller.class);
            String raw = ann.value();
            if (!raw.isBlank()) {
                segments.addFirst(resolveBasePathSegment(cur, raw));
            }
            cur = cur.getSuperclass();
        }
        return normalize(segments.isEmpty() ? "" : "/" + String.join("/", segments));
    }

    private static void validateSignature(Method m) {
        if (m.isSynthetic() || m.isBridge()) {
            return;
        }
        int mods = m.getModifiers();
        if (Modifier.isStatic(mods) || Modifier.isPrivate(mods)) {
            throw new JoltDIException(m.getName() + " must be public, non-static");
        }
    }

    private static RouteHandler createHandler(Object ctrl, Method m) {
        return ctx -> {
            if (ctrl instanceof BaseController baseCtrl) {
                baseCtrl.setContext(ctx);
            }
            Object[] args = Arrays.stream(m.getParameters())
                    .map(p -> resolveParam(p, ctx, m))
                    .toArray();
            try {
                return dispatchReturn(ctx, m.invoke(ctrl, args));
            } catch (InvocationTargetException e) {
                throw handleInvocationException(e, m);
            } catch (IllegalAccessException e) {
                throw new JoltRoutingException("Illegal access invoking " + m.getName(), e);
            }
        };
    }

    private static RuntimeException handleInvocationException(InvocationTargetException e, Method m) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException re) {
            return re;
        }
        if (cause instanceof Error er) {
            throw er;
        }
        return new JoltRoutingException("Error invoking " + m.getName(), cause);
    }

    private static Object resolveParam(Parameter p, JoltContext ctx, Method m) {
        if (p.isAnnotationPresent(Query.class) && p.getType() == Optional.class) {
            return resolveOptionalQuery(p, ctx);
        }
        if (p.isAnnotationPresent(Path.class)) {
            return processParamAnnotation(p, p.getAnnotation(Path.class), ctx);
        }
        if (p.isAnnotationPresent(Query.class)) {
            return processParamAnnotation(p, p.getAnnotation(Query.class), ctx);
        }
        if (p.isAnnotationPresent(Header.class)) {
            return processParamAnnotation(p, p.getAnnotation(Header.class), ctx);
        }
        if (p.isAnnotationPresent(Body.class)) {
            return ctx.body(p.getType());
        }
        if (p.isAnnotationPresent(ToForm.class)) {
            if (p.getType() == Form.class) {
                return ctx.form();
            }
            throw new JoltDIException("Cannot convert to form for type: " + p.getType());
        }
        if (JoltContext.class.isAssignableFrom(p.getType())) {
            return ctx;
        }
        throw new JoltDIException("Cannot resolve parameter: " + p.getName());
    }

    private static Object resolveOptionalQuery(Parameter p, JoltContext ctx) {
        Query queryAnn = p.getAnnotation(Query.class);
        String name = queryAnn.value().isEmpty() ? p.getName() : queryAnn.value();
        String raw = ctx.query(name);
        if (raw == null) {
            return Optional.empty();
        }
        Type type = p.getParameterizedType();
        if (type instanceof ParameterizedType pt) {
            Class<?> inner = (Class<?>) pt.getActualTypeArguments()[0];
            return Optional.ofNullable(HelpMethods.convert(raw, inner));
        }
        return Optional.of(raw);
    }

    private static JoltContext dispatchReturn(JoltContext ctx, Object result) {
        if (result == null) {
            return JoltDispatcherServlet.getCurrentContext();
        }
        if (result instanceof JoltContext jc) {
            return jc;
        }
        if (result instanceof ResponseEntity<?> resp) {
            return handleResponseEntity(ctx, resp);
        }
        if (result instanceof ModelView mv) {
            return ctx.render(mv.getView(), mv.getModel());
        }
        if (result instanceof String s) {
            return s.matches(".+\\.[a-z]{2,4}") ? ctx.serveStatic(s) : ctx.html(s);
        }
        return ctx.json(result);
    }

    private static JoltContext handleResponseEntity(JoltContext ctx, ResponseEntity<?> resp) {
        ctx.status(resp.getStatus());
        resp.getHeaders().forEach((name, values) -> values.forEach(value -> ctx.header(name, value)));
        if (resp.isRedirect()) {
            String location = resp.getHeaders().getOrDefault("Location", List.of()).stream().findFirst().orElse(null);
            if (resp.getBody() instanceof JoltModel model && location != null) {
                return ctx.render(location, model);
            }
            return location != null ? ctx.redirect(location) : ctx;
        }
        Object body = resp.getBody();
        if (body == null) {
            return ctx;
        }
        if (body instanceof ModelView mv) {
            return ctx.render(mv.getView(), mv.getModel());
        }
        if (body instanceof String str) {
            List<String> contentTypes = resp.getHeaders().get("Content-Type");
            return contentTypes != null && !contentTypes.isEmpty()
                    ? ctx.contentType(contentTypes.get(0)).write(str)
                    : ctx.html(str);
        }
        return ctx.json(body);
    }

    private static String invokeValue(Annotation a) {
        try {
            return (String) a.annotationType().getMethod("value").invoke(a);
        } catch (Exception e) {
            throw new JoltDIException("Failed to read @… value", e);
        }
    }

    private static String resolveBasePathSegment(Class<?> cls, String rawValue) {
        if (rawValue.contains("[controller]")) {
            String simple = cls.getSimpleName();
            String name = simple.endsWith("Controller")
                    ? simple.substring(0, simple.length() - "Controller".length())
                    : simple;
            return rawValue.replace("[controller]", name.toLowerCase());
        }
        return rawValue;
    }

    private static String normalize(String p) {
        String r = p.replaceAll("//+", "/");
        if (!r.startsWith("/")) {
            r = "/" + r;
        }
        return r.endsWith("/") && r.length() > 1 ? r.substring(0, r.length() - 1) : r;
    }

    private static String normalizePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private static Object processParamAnnotation(Parameter p, Annotation a, JoltContext ctx) {
        try {
            String paramName = ((String) a.annotationType().getMethod("value").invoke(a)).isEmpty()
                    ? p.getName()
                    : (String) a.annotationType().getMethod("value").invoke(a);
            String raw = getFromContextBasedOnType(paramName, a, ctx);
            return HelpMethods.convert(raw, p.getType());
        } catch (Exception e) {
            throw new JoltDIException("Failed to read 'value' from annotation: " + a.annotationType().getName(), e);
        }
    }

    private static String getFromContextBasedOnType(String paramName, Annotation a, JoltContext ctx) {
        if (a instanceof Query) {
            return ctx.query(paramName);
        } else if (a instanceof Path) {
            return ctx.path(paramName);
        } else if (a instanceof Header) {
            return ctx.header(paramName);
        }
        return null;
    }
}