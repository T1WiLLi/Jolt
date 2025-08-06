package io.github.t1willi.pipeline;

import io.github.t1willi.core.Router;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.routing.RouteMatch;
import io.github.t1willi.utils.MimeInterpreter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class StaticResourceStep implements PipelineStep {
    private final Router router;

    public StaticResourceStep() {
        this.router = JoltContainer.getInstance().getBean(Router.class);
    }

    @Override
    public boolean execute(ProcessingContext ctx) throws IOException, ServletException {
        if (!"GET".equals(ctx.getMethod())) {
            return false;
        }

        String path = ctx.getPath();
        RouteMatch match = router.match("GET", path);
        if (match != null && !isCatchAllRoute(match)) {
            return false;
        }

        String normalized = path.startsWith("/") ? path.substring(1) : path;
        String target = "/static/" + normalized;
        ServletContext sc = ctx.getRequest().getServletContext();

        if (sc.getResource(target) == null) {
            return false;
        }

        String ext = normalized.contains(".")
                ? normalized.substring(normalized.lastIndexOf('.') + 1)
                : "";
        String mime = sc.getMimeType(normalized);
        if (mime == null) {
            mime = MimeInterpreter.getMime(ext);
        }

        HttpServletResponse resp = ctx.getResponse();
        resp.setContentType(mime);

        HttpServletRequest req = ctx.getRequest();
        req.getRequestDispatcher(target).forward(req, resp);
        return true;
    }

    private boolean isCatchAllRoute(RouteMatch match) {
        String routePath = match.route().getPath();
        return routePath.matches("/\\*+");
    }
}
