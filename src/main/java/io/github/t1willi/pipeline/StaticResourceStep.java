package io.github.t1willi.pipeline;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import io.github.t1willi.core.Router;
import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.routing.RouteMatch;
import io.github.t1willi.utils.MimeInterpreter;
import jakarta.servlet.http.HttpServletResponse;

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
        InputStream in = null;

        File fs = new File("static", normalized);
        if (fs.isFile()) {
            in = new FileInputStream(fs);
        }

        if (in == null) {
            ServletContext sc = ctx.getRequest().getServletContext();
            in = sc.getResourceAsStream("/static/" + normalized);
        }

        if (in == null) {
            in = Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("static/" + normalized);
        }

        if (in == null) {
            return false;
        }

        try (InputStream resource = in) {
            byte[] data = resource.readAllBytes();
            String ext = normalized.contains(".")
                    ? normalized.substring(normalized.lastIndexOf('.') + 1)
                    : "";
            String mime = MimeInterpreter.getMime(ext);

            HttpServletResponse resp = ctx.getResponse();
            resp.setStatus(HttpStatus.OK.code());
            resp.setContentType(mime);
            resp.getOutputStream().write(data);
            return true;
        } catch (IOException e) {
            throw new JoltHttpException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error serving static resource: " + e.getMessage(),
                    e);
        }
    }

    private boolean isCatchAllRoute(RouteMatch match) {
        String path = match.route().getPath();
        return path.matches("/\\*+");
    }
}
