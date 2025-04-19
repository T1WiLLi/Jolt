package io.github.t1willi.core;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.t1willi.annotations.*;
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
                .orElse(List.of())
                .stream()
                .filter(b -> b.getClass().isAnnotationPresent(Controller.class))
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
        String base = normalizePath(ann.value().isBlank() ? "/" : ann.value());

        JoltContainer.getInstance().inject(controller);
        var router = Optional.ofNullable(JoltContainer.getInstance().getBean(Router.class))
                .orElseThrow(() -> new JoltDIException("Router bean not found"));

        router.before(ctx -> ((BaseController) controller).before(ctx), base);
        router.after(ctx -> ((BaseController) controller).after(ctx), base);
        for (var h : ((BaseController) controller).getSpecificBeforeHandlers())
            router.before(h.handler(), h.paths().stream().map(p -> normalizePath(base + p)).toArray(String[]::new));
        for (var h : ((BaseController) controller).getSpecificAfterHandlers())
            router.after(h.handler(), h.paths().stream().map(p -> normalizePath(base + p)).toArray(String[]::new));

        for (Method m : cls.getDeclaredMethods()) {
            List<RouteDef> defs = collectRoutes(m);
            for (RouteDef def : defs) {
                String full = normalizePath(base + def.path());
                router.route(def.method(), full, createHandler(controller, m));
            }
        }
    }

    private static List<RouteDef> collectRoutes(Method m) {
        int mods = m.getModifiers();
        if (!Modifier.isPublic(mods) || Modifier.isStatic(mods))
            return List.of();

        List<RouteDef> out = new ArrayList<>();
        record Pair(Class<? extends Annotation> A, HttpMethod M) {
        }
        List<Pair> map = List.of(
                new Pair(Get.class, HttpMethod.GET),
                new Pair(Post.class, HttpMethod.POST),
                new Pair(Put.class, HttpMethod.PUT),
                new Pair(Delete.class, HttpMethod.DELETE));

        for (var p : map) {
            if (m.isAnnotationPresent(p.A)) {
                Annotation ann = m.getAnnotation(p.A);
                String path = invokeValue(ann);
                out.add(new RouteDef(p.M, path));
            }
        }
        if (m.isAnnotationPresent(Mapping.class)) {
            Mapping mp = m.getAnnotation(Mapping.class);
            out.add(new RouteDef(mp.method(), mp.value()));
        }
        return out;
    }

    private static RouteHandler createHandler(Object controller, Method m) {
        return ctx -> {
            try {
                Object[] args = Arrays.stream(m.getParameters())
                        .map(p -> resolveParam(p, ctx))
                        .toArray();
                Object result = m.invoke(controller, args);
                return dispatchReturn(ctx, result);
            } catch (InvocationTargetException ite) {
                Throwable c = ite.getCause();
                if (c instanceof RuntimeException re)
                    throw re;
                throw new JoltRoutingException("Error in " + m.getName(), c);
            } catch (Exception e) {
                throw new JoltDIException("Cannot invoke " + m.getName(), e);
            }
        };
    }

    private static Object resolveParam(Parameter p, JoltContext ctx) {
        if (p.isAnnotationPresent(RequestPath.class)) {
            String key = p.getAnnotation(RequestPath.class).value();
            String raw = ctx.path(key);
            return HelpMethods.convert(raw, p.getType());
        }
        if (p.isAnnotationPresent(RequestQuery.class)) {
            String key = p.getAnnotation(RequestQuery.class).value();
            String raw = ctx.query(key);
            return HelpMethods.convert(raw, p.getType());
        }
        if (p.isAnnotationPresent(RequestForm.class)) {
            RequestForm rf = p.getAnnotation(RequestForm.class);
            String key = rf.value();
            if (key.isBlank()) {
                if (!Form.class.isAssignableFrom(p.getType()))
                    throw new JoltDIException("@RequestForm without name must be Form");
                return ctx.buildForm();
            }
            String raw = ctx.buildForm().getValue(key);
            return HelpMethods.convert(raw, p.getType());
        }
        if (p.isAnnotationPresent(RequestBody.class)) {
            RequestBody rb = p.getAnnotation(RequestBody.class);
            String key = rb.value();
            if (key.isBlank()) {
                if (p.getType().equals(String.class)) {
                    return ctx.bodyRaw();
                } else {
                    return ctx.body(p.getType());
                }
            }
            Map<String, Object> map = ctx.body(new TypeReference<>() {
            });
            Object v = map.get(key);
            return HelpMethods.convert(v == null ? null : v.toString(), p.getType());
        }
        if (JoltContext.class.isAssignableFrom(p.getType())) {
            return ctx;
        }
        throw new JoltDIException("Unsupported parameter “" + p.getName() + "”");
    }

    private static JoltContext dispatchReturn(JoltContext ctx, Object res) {
        if (res == null)
            return ctx;

        if (res instanceof JoltContext jc) {
            return jc;
        }
        if (res instanceof Template tpl) {
            return ctx.render(tpl.getView(), tpl.getModel());
        }
        if (res instanceof String s) {
            try {
                return ctx.serve(s);
            } catch (Exception ignore) {
                return ctx.html(s);
            }
        }
        return ctx.json(res);
    }

    private static String invokeValue(Annotation a) {
        try {
            return (String) a.annotationType().getMethod("value").invoke(a);
        } catch (Exception e) {
            throw new JoltDIException("Cannot read @… value", e);
        }
    }

    private static String normalizePath(String p) {
        String s = p.replaceAll("//+", "/");
        if (!s.startsWith("/"))
            s = "/" + s;
        if (s.endsWith("/") && s.length() > 1)
            s = s.substring(0, s.length() - 1);
        return s;
    }

    private record RouteDef(HttpMethod method, String path) {
    }
}