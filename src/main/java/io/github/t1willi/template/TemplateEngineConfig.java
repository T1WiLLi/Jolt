package io.github.t1willi.template;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Base configuration for template engines.
 * <p>
 * This class provides common configuration properties that can be used
 * across different template engine implementations.
 * 
 * @author William Beaudin
 * @since 2.6.6
 */
@Getter
@Setter
public class TemplateEngineConfig {
    /**
     * The base path for loading templates.
     * <p>
     * For class path resources, this would typically be something like
     * "/templates".
     */
    private String templatePath = "/templates";

    /**
     * Whether template caching should be enabled.
     */
    private boolean cachingEnabled = true;

    /**
     * The default encoding to use for templates.
     */
    private String defaultEncoding = "UTF-8";

    /**
     * Whether to log template exceptions.
     */
    private boolean logTemplateExceptions = true;

    /**
     * Global variables/functions to be made available in all templates.
     */
    private Map<String, Object> globalVariables = new HashMap<>();

    /**
     * Adds a global variable or function to be available in all templates.
     *
     * @param name  The name of the variable/function
     * @param value The implementation or value
     * @return This config instance for method chaining
     */
    public TemplateEngineConfig addGlobalVariable(String name, Object value) {
        globalVariables.put(name, value);
        return this;
    }

    /**
     * Creates a copy of this configuration.
     * 
     * @return A new configuration with the same settings
     */
    public TemplateEngineConfig copy() {
        TemplateEngineConfig copy = new TemplateEngineConfig();
        copy.templatePath = this.templatePath;
        copy.cachingEnabled = this.cachingEnabled;
        copy.defaultEncoding = this.defaultEncoding;
        copy.logTemplateExceptions = this.logTemplateExceptions;
        copy.globalVariables = new HashMap<>(this.globalVariables);
        return copy;
    }
}
