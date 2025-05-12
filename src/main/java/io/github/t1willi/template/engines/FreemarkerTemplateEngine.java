package io.github.t1willi.template.engines;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import freemarker.core.XHTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;
import io.github.t1willi.exceptions.JoltHttpException;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.injector.annotation.Bean;
import io.github.t1willi.security.nonce.Nonce;
import io.github.t1willi.template.JoltModel;
import io.github.t1willi.template.TemplateEngine;
import io.github.t1willi.template.TemplateEngineConfig;

/**
 * Freemarker implementation of the templating engine.
 * 
 * @author William Beaudin
 * @since 2.6.6
 */
@Bean
public class FreemarkerTemplateEngine implements TemplateEngine {

    private static final Logger LOGGER = Logger.getLogger(FreemarkerTemplateEngine.class.getName());
    private static final String ENGINE_NAME = "Freemarker";
    private static final String FILE_EXTENSION = ".ftl";

    private Configuration configuration;
    private boolean initialized = false;

    @Override
    public String render(String templateName, JoltModel model) throws JoltHttpException {
        if (!initialized) {
            throw new IllegalStateException("Freemarker engine has not been initialized!");
        }

        try {
            Template template = configuration.getTemplate(templateName + FILE_EXTENSION);
            StringWriter writer = new StringWriter();
            template.process(model.asMap(), writer);
            return writer.toString();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Template not found: " + templateName, e);
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Template not found: " + templateName, e);
        } catch (TemplateException e) {
            LOGGER.log(Level.SEVERE, "Error processing template: " + templateName, e);
            throw new JoltHttpException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error processing template: " + templateName, e);
        }
    }

    @Override
    public void initialize(TemplateEngineConfig config) {
        this.configuration = new Configuration(Configuration.VERSION_2_3_32);
        this.configuration.setDefaultEncoding(config.getDefaultEncoding());
        this.configuration.setTemplateExceptionHandler(
                config.isLogTemplateExceptions() ? TemplateExceptionHandler.RETHROW_HANDLER
                        : TemplateExceptionHandler.IGNORE_HANDLER);
        this.configuration.setLogTemplateExceptions(config.isLogTemplateExceptions());
        this.configuration.setWrapUncheckedExceptions(true);
        this.configuration.setOutputFormat(XHTMLOutputFormat.INSTANCE);

        this.configuration.setTemplateUpdateDelayMilliseconds(
                config.isCachingEnabled() ? 3600000 : 0);

        this.configuration.setClassLoaderForTemplateLoading(
                this.getClass().getClassLoader(),
                config.getTemplatePath());

        for (Map.Entry<String, Object> entry : config.getGlobalVariables().entrySet()) {
            try {
                this.configuration.setSharedVariable(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING,
                        "Failed to register global variable: " + entry.getKey(), e);
            }
        }
        this.configuration.setSharedVariable("nonce", new NonceDirective());
        this.initialized = true;
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
        return this.initialized;
    }

    private static class NonceDirective implements TemplateMethodModelEx {
        private static final Logger logger = Logger.getLogger(NonceDirective.class.getName());

        @Override
        public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {
            if (!arguments.isEmpty()) {
                throw new TemplateModelException("nonce() method does not accept arguments");
            }

            String nonce = Nonce.get();
            if (nonce == null) {
                logger.warning("Failed to generate nonce for template");
                throw new TemplateModelException("Failed to generate nonce for template");
            }

            return new SimpleScalar(nonce);
        }
    }
}
