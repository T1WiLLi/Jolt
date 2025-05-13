package io.github.t1willi.context;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.injector.JoltContainer;
import io.github.t1willi.localization.LanguageService;
import io.github.t1willi.security.config.SecurityConfiguration;
import io.github.t1willi.security.csrf.CsrfToken;
import io.github.t1willi.template.JoltModel;
import io.github.t1willi.template.TemplateConfiguration;
import io.github.t1willi.template.TemplateEngine;

/**
 * Handles template rendering with security features like CSRF token injection.
 */
final class TemplatingContext {
    private static final Logger logger = Logger.getLogger(TemplatingContext.class.getName());
    private static final Pattern FORM_PATTERN = Pattern.compile("<form\\b[^>]*>", Pattern.CASE_INSENSITIVE);

    private final TemplateEngine templateEngine;

    /**
     * Creates a new TemplatingContext using the configured default template engine.
     */
    public TemplatingContext() {
        TemplateConfiguration config = JoltContainer.getInstance().getBean(TemplateConfiguration.class);
        this.templateEngine = config.getEngine();
    }

    /**
     * Renders a template with the provided model and injects security tokens.
     *
     * @param response     The response context to write to
     * @param templatePath The path to the template
     * @param model        The data model for the template
     */
    public void render(ResponseContext response, String templatePath, JoltModel model) {
        // Get or create the base language model
        JoltModel baseModel = LanguageService.getGlobalLanguageModel();
        if (baseModel == null) {
            baseModel = JoltModel.create();
        }

        // Generate CSRF token
        String csrfToken = CsrfToken.generate();
        String tokenName = JoltContainer.getInstance().getBean(SecurityConfiguration.class)
                .getCsrfConfig()
                .getTokenName();

        // Merge models if provided
        JoltModel finalModel = model != null ? baseModel.merge(model) : baseModel;

        // Render template
        String renderedHtml = renderTemplate(templatePath, finalModel);

        // Inject CSRF tokens into forms
        renderedHtml = injectCsrfTokenIntoForms(renderedHtml, csrfToken, tokenName);

        // Set response content
        response.setContentType("text/html; charset=UTF-8");
        response.html(renderedHtml);
    }

    /**
     * Renders a template using the configured template engine.
     *
     * @param templatePath The path to the template
     * @param model        The data model for the template
     * @return The rendered HTML
     * @throws JoltHttpException If an error occurs during rendering
     */
    private String renderTemplate(String templatePath, JoltModel model) {
        try {
            return templateEngine.render(templatePath, model);
        } catch (Exception e) {
            logger.severe("Error rendering template " + templatePath + ": " + e.getMessage());
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error while rendering template: " + templatePath, e);
        }
    }

    /**
     * Injects CSRF tokens into forms for security.
     *
     * @param html      The HTML to inject tokens into
     * @param csrfToken The CSRF token
     * @param tokenName The name of the CSRF token field
     * @return The HTML with CSRF tokens injected
     */
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