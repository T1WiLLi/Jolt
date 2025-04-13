package io.github.t1willi.routing.context;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.logging.Logger;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.localization.LanguageService;
import io.github.t1willi.template.JoltModel;
import io.github.t1willi.template.TemplateConfiguration;

final class TemplatingContext {
    private static final Logger logger = Logger.getLogger(TemplatingContext.class.getName());

    private final Configuration configuration;

    public TemplatingContext() {
        this.configuration = JoltContainer.getInstance().getBean(TemplateConfiguration.class).getConfiguration();
    }

    public void render(ResponseContext response, String templatePath, JoltModel model) {
        JoltModel baseModel = LanguageService.getGlobalLanguageModel();
        if (baseModel == null) {
            baseModel = JoltModel.create();
        }
        JoltModel finalModel = model != null ? baseModel.merge(model) : baseModel;
        render(response, templatePath, finalModel.asMap());
    }

    private void render(ResponseContext response, String templatePath, Map<String, Object> dataModel) {
        try {
            Template template = configuration.getTemplate(templatePath + ".ftl"); // .ftl is the default extension
            response.setContentType("text/html; charset=UTF-8");
            StringWriter stringWriter = new StringWriter();
            template.process(dataModel, stringWriter);
            response.html(stringWriter.toString());
        } catch (IOException | TemplateException e) {
            logger.severe("Error rendering template " + templatePath + ": " + e.getMessage());
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error while rendering template: " + templatePath, e);
        }
    }
}