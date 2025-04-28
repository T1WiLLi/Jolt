package io.github.t1willi.pipeline;

import java.io.IOException;

import io.github.t1willi.core.Router;
import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.http.HttpMethod;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.routing.RouteMatch;
import jakarta.servlet.ServletException;

public class RoutingStep implements PipelineStep {
    private final Router router;

    public RoutingStep() {
        this.router = JoltContainer.getInstance().getBean(Router.class);
    }

    @Override
    public boolean execute(ProcessingContext context) throws IOException, ServletException {
        RouteMatch match = router.match(context.getMethod(), context.getPath());
        context.setMatch(match);

        if (match != null) {
            return false;
        }

        if (!HttpMethod.GET.name().equals(context.getMethod())) {
            if (router.pathExistsWithDifferentMethod(context.getMethod(), context.getPath())) {
                context.getResponse().setHeader("Allow", router.getAllowedMethods(context.getPath()));
                throw new JoltHttpException(HttpStatus.METHOD_NOT_ALLOWED,
                        "Method not allowed for " + context.getPath());
            }
            throw new JoltHttpException(HttpStatus.NOT_FOUND,
                    "No route or static resource found for " + context.getPath());
        }
        return false;
    }
}
