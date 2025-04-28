package io.github.t1willi.context;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.localization.LanguageService;
import io.github.t1willi.security.config.SecurityConfiguration;
import io.github.t1willi.security.csrf.CsrfToken;
import io.github.t1willi.template.JoltModel;
import io.github.t1willi.template.TemplateConfiguration;

final class TemplatingContext {
    private static final Logger logger = Logger.getLogger(TemplatingContext.class.getName());
    private static final Pattern FORM_PATTERN = Pattern.compile("<form\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    private final Configuration configuration;

    public TemplatingContext() {
        this.configuration = JoltContainer.getInstance().getBean(TemplateConfiguration.class).getConfiguration();
    }

    public void render(ResponseContext response, String templatePath, JoltModel model) {
        JoltModel baseModel = LanguageService.getGlobalLanguageModel();
        if (baseModel == null) {
            baseModel = JoltModel.create();
        }

        String csrfToken = CsrfToken.generate();
        String tokenName = JoltContainer.getInstance().getBean(SecurityConfiguration.class).getCsrfConfig()
                .getTokenName();

        JoltModel finalModel = model != null ? baseModel.merge(model) : baseModel;
        String renderedHtml = renderTemplate(templatePath, finalModel.asMap());
        renderedHtml = injectCsrfTokenIntoForms(renderedHtml, csrfToken, tokenName);
        response.setContentType("text/html; charset=UTF-8");
        response.html(renderedHtml);
    }

    private String renderTemplate(String templatePath, Map<String, Object> dataModel) {
        try {
            Template template = configuration.getTemplate(templatePath + ".ftl");
            StringWriter stringWriter = new StringWriter();
            template.process(dataModel, stringWriter);
            return stringWriter.toString();
        } catch (IOException | TemplateException e) {
            logger.severe("Error rendering template " + templatePath + ": " + e.getMessage());
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error while rendering template: " + templatePath, e);
        }
    }

    private String injectCsrfTokenIntoForms(String html, String csrfToken, String tokenName) {
        if (csrfToken == null || tokenName == null) {
            logger.fine("CSRF token or token name not available, skipping form injection");
            return html;
        }

        StringBuilder modifiedHtml = new StringBuilder();
        Matcher matcher = FORM_PATTERN.matcher(html);
        int lastEnd = 0;

        while (matcher.find()) {
            int end = matcher.end();

            modifiedHtml.append(html, lastEnd, end);

            String formTag = matcher.group();
            String formTagLower = formTag.toLowerCase();
            if (formTagLower.contains("method=\"post\"") ||
                    formTagLower.contains("method=\"put\"") ||
                    formTagLower.contains("method=\"delete\"") ||
                    formTagLower.contains("method=\"patch\"")) {
                modifiedHtml.append("<input type=\"hidden\" name=\"")
                        .append(tokenName)
                        .append("\" value=\"")
                        .append(csrfToken)
                        .append("\">");
                logger.fine(() -> "Injected CSRF token into form: " + formTag);
            }

            lastEnd = end;
        }

        modifiedHtml.append(html.substring(lastEnd));
        return modifiedHtml.toString();
    }
}