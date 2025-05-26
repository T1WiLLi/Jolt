package io.github.t1willi.openapi;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.github.t1willi.annotations.Delete;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.annotations.Mapping;
import io.github.t1willi.annotations.Post;
import io.github.t1willi.annotations.Put;
import io.github.t1willi.core.BaseController;
import io.github.t1willi.core.ControllerRegistry;
import io.github.t1willi.core.JoltApplication;
import io.github.t1willi.core.Router;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.injector.annotation.Bean;
import io.github.t1willi.openapi.annotations.Docs;
import io.github.t1willi.openapi.annotations.OpenApi;
import io.github.t1willi.openapi.models.OpenApiModel;
import jakarta.annotation.PostConstruct;

@Bean
public class OpenapiService {

    private static final Logger logger = Logger.getLogger(OpenapiService.class.getName());

    private final boolean enabled = JoltApplication.openApi() != null;
    private volatile boolean initialized = false;
    private volatile OpenApiModel model = null;

    @PostConstruct
    public void init() {
        if (enabled) {
            OpenApi openApi = JoltApplication.openApi();
            Thread.startVirtualThread(() -> {
                try {
                    Thread.sleep(10 * 60 * 1000); // 10 seconds
                    model = OpenApiModel.of(openApi, getMethods());
                    initialized = true;
                    logger.info("OpenAPI model initialized successfully");
                } catch (Exception e) {
                    logger.warning(() -> "Error initializing OpenAPI model: " + e.getMessage());
                }
                createEndpoint();
            });
        }
    }

    private List<Map.Entry<Docs, Method>> getMethods() {
        List<Map.Entry<Docs, Method>> result = new ArrayList<>();
        List<BaseController> controllers = ControllerRegistry.getControllers();

        for (BaseController baseController : controllers) {
            List<Method> methods = filterMethods(baseController.getClass().getMethods());
            result.addAll(
                    methods.stream()
                            .filter(method -> method.isAnnotationPresent(Docs.class))
                            .map(method -> Map.entry(method.getAnnotation(Docs.class), method))
                            .collect(Collectors.toList()));
        }
        return List.of();
    }

    private void createEndpoint() {
        Router router = JoltContainer.getInstance().getBean(Router.class);
        String path = JoltApplication.openApi() != null ? JoltApplication.openApi().path() : "/openapi.json";

        router.get(path, ctx -> {
            if (!initialized || model == null) {
                return ctx.json(Map.of("message", "OpenAPI is loading, please wait..."));
            }
            return ctx.json(model);
        });
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
}
