package io.github.t1willi.routing.context;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.template.JoltModel;
import io.github.t1willi.template.TemplateConfiguration;

/**
 * Represents the templating context, providing utility methods to access
 * various details about the templating context.
 * 
 * @author William Beaudin.
 * @since 2.0
 */
final class TemplatingContext {
    private final Configuration configuration;

    public TemplatingContext() {
        this.configuration = JoltContainer.getInstance().getBean(TemplateConfiguration.class).getConfiguration();
    }

    public void render(ResponseContext response, String templatePath, JoltModel model) {
        render(response, templatePath, model.asMap());
    }

    private void render(ResponseContext response, String templatePath, Map<String, Object> dataModel) {
        try {
            Template template = configuration.getTemplate(templatePath);
            response.setContentType("text/html; charset=UTF-8");
            StringWriter stringWriter = new StringWriter();
            template.process(dataModel, stringWriter);
            response.html(stringWriter.toString());
        } catch (IOException | TemplateException e) {
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error while rendering template: " + templatePath, e);
        }
    }
}