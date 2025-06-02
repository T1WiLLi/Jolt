package io.github.t1willi.context;

import java.util.logging.Level;
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
import io.github.t1willi.utils.Flash;

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
        JoltModel baseModel = LanguageService.getGlobalLanguageModel();
        if (baseModel == null) {
            baseModel = JoltModel.empty();
        }

        JoltModel finalModel = baseModel.clone();

        injectFlashIntoModel(finalModel);

        if (model != null) {
            finalModel.merge(model);
        }

        String csrfToken = CsrfToken.generate();
        String tokenName = JoltContainer.getInstance().getBean(SecurityConfiguration.class)
                .getCsrfConfig()
                .getTokenName();

        try {
            String renderedHtml = renderTemplate(templatePath, finalModel);
            renderedHtml = injectCsrfTokenIntoForms(renderedHtml, csrfToken, tokenName);
            response.setContentType("text/html; charset=UTF-8");
            response.html(renderedHtml);
            logger.log(Level.FINE, () -> "Rendered template: " + templatePath +
                    " with model keys: " + String.join(", ", finalModel.getKeys()));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to render template: " + templatePath, e);
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error while rendering template: " + templatePath, e);
        }
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
            logger.fine("No CSRF token or token name, skipping injection");
            return html;
        }

        StringBuilder out = new StringBuilder();
        Matcher m = FORM_PATTERN.matcher(html);
        int lastEnd = 0;

        while (m.find()) {
            String formTag = m.group();
            String lower = formTag.toLowerCase();

            out.append(html, lastEnd, m.end());

            boolean isStateful = lower.contains("method=\"post\"")
                    || lower.contains("method=\"put\"")
                    || lower.contains("method=\"delete\"")
                    || lower.contains("method=\"patch\"")
                    || lower.contains("hx-post")
                    || lower.contains("hx-put")
                    || lower.contains("hx-delete")
                    || lower.contains("hx-patch");

            if (isStateful) {
                out.append("\n  <input type=\"hidden\" ")
                        .append("name=\"").append(tokenName).append("\" ")
                        .append("value=\"").append(csrfToken).append("\">");
                logger.fine(() -> "Injected CSRF into: " + formTag);
            }

            lastEnd = m.end();
        }

        out.append(html.substring(lastEnd));
        return out.toString();
    }

    private void injectFlashIntoModel(JoltModel model) {
        FlashWrapper flashWrapper = new FlashWrapper();
        model.with("flash", flashWrapper);
    }

    public static final class FlashWrapper {
        public String message() {
            return Flash.message();
        }

        public String type() {
            return Flash.type();
        }

        public boolean has() {
            return Flash.has();
        }
    }
}