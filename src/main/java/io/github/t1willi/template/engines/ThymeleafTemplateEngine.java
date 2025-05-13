package io.github.t1willi.template.engines;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.engine.AttributeName;
import org.thymeleaf.exceptions.TemplateEngineException;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.processor.element.AbstractAttributeTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.security.nonce.Nonce;
import io.github.t1willi.template.JoltModel;
import io.github.t1willi.template.TemplateEngineConfig;

/**
 * Thymeleaf implementation of the TemplateEngine interface.
 * 
 * @author William Beaudin
 * @since 2.1
 */
// TODO: Add support for global variables and functions like freemarker has.
public class ThymeleafTemplateEngine implements io.github.t1willi.template.TemplateEngine {

    private static final Logger LOGGER = Logger.getLogger(ThymeleafTemplateEngine.class.getName());
    private static final String ENGINE_NAME = "Thymeleaf";
    private static final String FILE_EXTENSION = ".html";

    private TemplateEngine templateEngine;
    private boolean initialized = false;

    @Override
    public String render(String templateName, JoltModel model) throws JoltHttpException {
        if (!initialized) {
            throw new IllegalStateException("Thymeleaf template engine has not been initialized");
        }

        try {
            IContext context = createContext(model);
            return templateEngine.process(templateName + FILE_EXTENSION, context);
        } catch (TemplateEngineException e) {
            LOGGER.log(Level.SEVERE, "Error processing template: " + templateName, e);
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error processing template: " + templateName, e);
        }
    }

    @Override
    public void initialize(TemplateEngineConfig config) {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setPrefix(config.getTemplatePath() + "/");
        templateResolver.setSuffix(FILE_EXTENSION);
        templateResolver.setCharacterEncoding(config.getDefaultEncoding());
        templateResolver.setCacheable(config.isCachingEnabled());
        templateResolver.setCacheTTLMs(3600000L);

        this.templateEngine = new TemplateEngine();
        this.templateEngine.setTemplateResolver(templateResolver);
        this.templateEngine.addDialect(new SecurityDialect());

        this.initialized = true;
    }

    private IContext createContext(JoltModel model) {
        Context context = new Context();
        for (Map.Entry<String, Object> entry : model.asMap().entrySet()) {
            context.setVariable(entry.getKey(), entry.getValue());
        }
        return context;
    }

    @Override
    public String getName() {
        return ENGINE_NAME;
    }

    @Override
    public String getFileExtension() {
        return FILE_EXTENSION;
    }

    @Override
    public boolean isInitialized() {
        return initialized;
    }

    private static class NonceAttributeProcessor extends AbstractAttributeTagProcessor {

        private static final String NONCE_ATTRIBUTE = "nonce";
        private static final int PRECEDENCE = 1000;

        public NonceAttributeProcessor(String dialectPrefix) {
            super(
                    TemplateMode.HTML,
                    dialectPrefix,
                    null,
                    false,
                    NONCE_ATTRIBUTE,
                    true,
                    PRECEDENCE,
                    true);
        }

        @Override
        protected void doProcess(ITemplateContext context, IProcessableElementTag tag, AttributeName attributeName,
                String attributeValue, IElementTagStructureHandler structureHandler) {
            String nonceValue = Nonce.get();
            structureHandler.setAttribute(attributeName.getAttributeName(),
                    org.unbescape.html.HtmlEscape.escapeHtml5(nonceValue));
        }
    }

    private static class SecurityDialect extends AbstractProcessorDialect {
        private static final String DIALECT_NAME = "SecurityDialect";
        private static final String DIALECT_PREFIX = "sec";

        public SecurityDialect() {
            super(DIALECT_NAME, DIALECT_PREFIX, 1000);
        }

        @Override
        public Set<IProcessor> getProcessors(String dialectPrefix) {
            return Collections.singleton(new NonceAttributeProcessor(dialectPrefix));
        }
    }
}