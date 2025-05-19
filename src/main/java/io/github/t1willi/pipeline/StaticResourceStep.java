package io.github.t1willi.pipeline;

import java.io.IOException;
import java.io.InputStream;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.utils.MimeInterpreter;
import jakarta.servlet.http.HttpServletResponse;

public class StaticResourceStep implements PipelineStep {
    @Override
    public boolean execute(ProcessingContext ctx) throws IOException, ServletException {
        if (!"GET".equals(ctx.getMethod())) {
            return false;
        }

        String path = ctx.getPath();
        String normalized = path.startsWith("/")
                ? path.substring(1)
                : path;

        ServletContext sc = ctx.getRequest().getServletContext();
        try (InputStream in = sc.getResourceAsStream("/static/" + normalized)) {
            if (in == null) {
                return false;
            }
            byte[] data = in.readAllBytes();

            int dot = normalized.lastIndexOf('.');
            String ext = (dot != -1) ? normalized.substring(dot + 1) : "";
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
}
