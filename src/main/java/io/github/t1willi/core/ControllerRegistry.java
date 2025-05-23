package io.github.t1willi.core;

import io.github.t1willi.annotations.Body;
import io.github.t1willi.annotations.Controller;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.annotations.Mapping;
import io.github.t1willi.annotations.Post;
import io.github.t1willi.annotations.Put;
import io.github.t1willi.annotations.Delete;
import io.github.t1willi.annotations.Path;
import io.github.t1willi.annotations.Query;
import io.github.t1willi.annotations.ToForm;
import io.github.t1willi.annotations.Version;
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
import io.github.t1willi.security.authentification.RouteRule;
import io.github.t1willi.template.JoltModel;
import io.github.t1willi.utils.HelpMethods;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public final class ControllerRegistry {
    private static final Logger log = Logger.getLogger(ControllerRegistry.class.getName());
    public static final List<RouteRule> AUTHORIZATION = new ArrayList<>();

    private ControllerRegistry() {
    }

    public static void registerControllers() {
        List<BaseController> controllers;
        try {
            controllers = Optional.ofNullable(
                    JoltContainer.getInstance().getBeans(BaseController.class))
                    .orElse(List.of()).stream()
                    .filter(bean -> bean.getClass().isAnnotationPresent(Controller.class))
                    .toList();
        } catch (Exception e) {
            log.warning("No @Controller beans found.");
            return;
        }

        for (BaseController controller : controllers) {
            registerController(controller);
        }
    }

    private static void registerController(BaseController controller) {
        Class<?> cls = controller.getClass();
        String basePath = computeHierarchicalBasePath(cls);
        Version classVerAnn = cls.getAnnotation(Version.class);
        int classVersion = classVerAnn != null ? classVerAnn.value() : 0;
        String classPrefix = classVerAnn != null ? classVerAnn.prefix() : "v";

        JoltContainer.getInstance().inject(controller);
        Router router = Optional.ofNullable(
                JoltContainer.getInstance().getBean(Router.class))
                .orElseThrow(() -> new JoltDIException("Router bean not found"));

        registerLifecycles(controller, router, basePath);
        Authorize classAuth = cls.getAnnotation(Authorize.class);

        for (Method method : filterMethods(cls.getMethods())) {
            validateSignature(method);
            registerHttpRoutes(controller, method, router, basePath, classAuth, classVersion, classPrefix);
            registerMappingRoute(controller, method, router, basePath, classAuth, classVersion, classPrefix);
        }
    }

    private static List<Method> filterMethods(Method[] methods) {
        return Arrays.asList(methods).stream()
                .filter(method -> method.isAnnotationPresent(Get.class) ||
                        method.isAnnotationPresent(Post.class) ||
                        method.isAnnotationPresent(Put.class) ||
                        method.isAnnotationPresent(Delete.class) ||
                        method.isAnnotationPresent(Mapping.class))
                .toList();
    }

    private static void registerLifecycles(BaseController controller, Router router, String basePath) {
        router.before(controller::before, basePath);
        router.after(controller::after, basePath);

        controller.getSpecificBeforeHandlers().forEach(h -> router.before(
                h.handler(),
                h.paths().stream()
                        .map(p -> normalize(basePath + p))
                        .toArray(String[]::new)));
        controller.getSpecificAfterHandlers().forEach(h -> router.after(
                h.handler(),
                h.paths().stream()
                        .map(p -> normalize(basePath + p))
                        .toArray(String[]::new)));
    }

    private static void registerHttpRoutes(
            Object controller,
            Method method,
            Router router,
            String basePath,
            Authorize classAuth,
            int classVersion,
            String classPrefix) {
        Map<Class<? extends Annotation>, HttpMethod> httpMethodAnnotations = Map.of(
                Get.class, HttpMethod.GET,
                Post.class, HttpMethod.POST,
                Put.class, HttpMethod.PUT,
                Delete.class, HttpMethod.DELETE);
        httpMethodAnnotations.forEach((annType, verb) -> {
            if (!method.isAnnotationPresent(annType))
                return;
            Annotation ann = method.getAnnotation(annType);
            Version verAnn = method.getAnnotation(Version.class);
            int methodVersion = verAnn != null ? verAnn.value() : 0;
            String methodPrefix = verAnn != null ? verAnn.prefix() : classPrefix;
            int effectiveVersion = methodVersion > 0 ? methodVersion : classVersion;

            String rawPath = invokeValue(ann);
            String fullPath = buildPath(basePath, rawPath, effectiveVersion, methodPrefix);
            router.route(verb, fullPath, createHandler(controller, method));
            applyAuthorization(method, classAuth, fullPath, verb);
        });
    }

    private static void registerMappingRoute(
            Object controller,
            Method method,
            Router router,
            String basePath,
            Authorize classAuth,
            int classVersion,
            String classPrefix) {
        if (!method.isAnnotationPresent(Mapping.class))
            return;
        Mapping mapping = method.getAnnotation(Mapping.class);
        Version verAnn = method.getAnnotation(Version.class);
        int methodVersion = verAnn != null ? verAnn.value() : 0;
        String methodPrefix = verAnn != null ? verAnn.prefix() : classPrefix;
        int effectiveVersion = methodVersion > 0 ? methodVersion : classVersion;

        String fullPath = buildPath(basePath, mapping.value(), effectiveVersion, methodPrefix);
        router.route(mapping.method(), fullPath, createHandler(controller, method));
        processAuthorizationWithContainer(method, classAuth, fullPath, mapping.method());
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
            if (!raw.isBlank())
                segments.addFirst(resolveBasePathSegment(cur, raw));
            cur = cur.getSuperclass();
        }
        return normalize(segments.isEmpty() ? "" : "/" + String.join("/", segments));
    }

    private static void applyAuthorization(
            Method method,
            Authorize classAuth,
            String fullPath,
            HttpMethod verb) {
        Authorize methodAuth = method.getAnnotation(Authorize.class);
        Authorize effective = methodAuth != null ? methodAuth : classAuth;
        if (effective == null)
            return;
        try {
            AuthStrategy strat = effective.value().getDeclaredConstructor().newInstance();
            AUTHORIZATION.add(new RouteRule(fullPath, false, Set.of(verb.name()), strat, false, false));
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

    private static void validateSignature(Method m) {
        if (m.isSynthetic() || m.isBridge()) {
            return;
        }
        int mods = m.getModifiers();
        if (Modifier.isStatic(mods) || Modifier.isPrivate(mods)) {
            throw new JoltDIException(m.getName() + "must be public, non-static");
        }
    }

    private static RouteHandler createHandler(Object ctrl, Method m) {
        return ctx -> {
            if (ctrl instanceof BaseController baseCtrl) {
                baseCtrl.setContext(ctx);
            }
            try {
                Object[] args = Arrays.stream(m.getParameters())
                        .map(p -> resolveParam(p, ctx, m))
                        .toArray();
                Object result = m.invoke(ctrl, args);
                return dispatchReturn(ctx, result);
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getCause();
                if (cause instanceof RuntimeException re) {
                    throw re;
                }
                if (cause instanceof Error er) {
                    throw er;
                }
                throw new JoltRoutingException("Error invoking " + m.getName(), cause);
            } catch (IllegalAccessException e) {
                throw new JoltRoutingException("Illegal access invoking " + m.getName(), e);
            } catch (Exception e) {
                throw e;
            }
        };
    }

    private static Object resolveParam(Parameter p, JoltContext ctx, Method m) {
        if (p.isAnnotationPresent(Query.class) && p.getType() == Optional.class) {
            Query queryAnn = p.getAnnotation(Query.class);
            String name = queryAnn.value().isEmpty() ? p.getName() : queryAnn.value();
            String raw = ctx.query(name);
            if (raw == null) {
                return Optional.empty();
            }
            Type type = p.getParameterizedType();
            if (type instanceof ParameterizedType pt) {
                Type arg = pt.getActualTypeArguments()[0];
                Class<?> inner = (Class<?>) arg;
                Object converted = HelpMethods.convert(raw, inner);
                return Optional.ofNullable(converted);
            }
            return Optional.of(raw);
        }

        if (p.isAnnotationPresent(Path.class)) {
            Path pathAnn = p.getAnnotation(Path.class);
            String paramName = pathAnn.value().isEmpty() ? p.getName() : pathAnn.value();
            String raw = ctx.path(paramName);
            return HelpMethods.convert(raw, p.getType());
        }

        if (p.isAnnotationPresent(Query.class)) {
            Query queryAnn = p.getAnnotation(Query.class);
            String paramName = queryAnn.value().isEmpty() ? p.getName() : queryAnn.value();
            String raw = ctx.query(paramName);
            return HelpMethods.convert(raw, p.getType());
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

        throw new JoltDIException("Cannot resolve parameter: " + p.getName() +
                ". Must be annotated with @Path, @Query, @Body, @ToForm or be JoltContext");
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
            if (s.matches(".+\\.[a-z]{2,4}")) {
                return ctx.serveStatic(s);
            }
            return ctx.html(s);
        }
        return ctx.json(result);
    }

    private static JoltContext handleResponseEntity(JoltContext ctx, ResponseEntity<?> resp) {
        ctx.status(resp.getStatus());
        resp.getHeaders().forEach((name, values) -> values.forEach(value -> ctx.header(name, value)));
        if (resp.isRedirect()) {
            String location = resp.getHeaders()
                    .getOrDefault("Location", List.of())
                    .stream().findFirst().orElse(null);
            if (resp.getBody() instanceof JoltModel model) {
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
            if (contentTypes != null && !contentTypes.isEmpty()) {
                ctx.contentType(contentTypes.get(0));
                return ctx.write(str);
            }
            return ctx.html(str);
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
            String segment = name.toLowerCase();
            return rawValue.replace("[controller]", segment);
        }
        return rawValue;
    }

    private static String normalize(String p) {
        String r = p.replaceAll("//+", "/");
        if (!r.startsWith("/"))
            r = "/" + r;
        if (r.endsWith("/") && r.length() > 1)
            r = r.substring(0, r.length() - 1);
        return r;
    }

    private static String normalizePath(String path) {
        if (!path.startsWith("/")) {
            return "/" + path;
        }
        return path;
    }

}
