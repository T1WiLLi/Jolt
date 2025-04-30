package io.github.t1willi.pipeline;

import java.io.IOException;
import java.io.InputStream;

import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.http.HttpMethod;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.utils.DirectoryListingHtmlTemplateBuilder;
import io.github.t1willi.utils.HelpMethods;
import io.github.t1willi.utils.MimeInterpreter;
import jakarta.servlet.ServletException;

public class StaticResourceStep implements PipelineStep {

    @Override
    public boolean execute(ProcessingContext context) throws IOException, ServletException {
        if (!HttpMethod.GET.name().equals(context.getMethod()) || context.getMatch() != null) {
            return false;
        }

        String path = context.getPath();
        if (!HelpMethods.isValidStaticResourcePath(path)) {
            throw new JoltHttpException(HttpStatus.NOT_FOUND,
                    "No route or static resource found for " + context.getPath());
        }

        String clean = path.replaceAll("\\.\\./", "").replaceAll("\\.\\.\\\\", "");
        String resourcePath = "static/" + clean.replaceAll("\\.\\./", "").replaceAll("\\.\\.\\\\", "");
        InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (in == null && !clean.contains(".")) {
            String idx = resourcePath + (resourcePath.endsWith("/") ? "" : "/") + "index.html";
            in = getClass().getClassLoader().getResourceAsStream(idx);
            if (in != null) {
                resourcePath = idx;
            }
        }

        if (in != null) {
            byte[] data = in.readAllBytes();
            String ext = resourcePath.substring(resourcePath.lastIndexOf('.'));
            context.getResponse().setContentType(MimeInterpreter.getMime(ext));
            context.getResponse().getOutputStream().write(data);
            return true;
        }

        if (DirectoryListingHtmlTemplateBuilder.tryServeDirectoryListing(path, context.getResponse())) {
            return true;
        }

        throw new JoltHttpException(HttpStatus.NOT_FOUND,
                "No route or static resource found for " + context.getPath());
    }
}
