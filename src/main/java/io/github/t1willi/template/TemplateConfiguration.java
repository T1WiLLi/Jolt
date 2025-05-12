package io.github.t1willi.template;

import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import lombok.Getter;

/**
 * Defines the configuration mechanism for template engines in the Jolt
 * framework.
 * <p>
 * This class allows configuring template engines and provides access
 * to the template engine registry.
 * 
 * @author William Beaudin.
 * @since 2.0
 */
public abstract class TemplateConfiguration {

    /**
     * The configuration for template engines.
     */
    @Getter
    protected final TemplateEngineConfig engineConfig;

    /**
     * The registry for template engines.
     */
    @Getter
    protected final TemplateEngineRegistry engineRegistry;

    /**
     * Map of custom functions/methods to be added to templates.
     */
    protected final Map<String, Object> globalFunctions = new HashMap<>();

    /**
     * Creates a new template configuration.
     */
    public TemplateConfiguration() {
        this.engineConfig = new TemplateEngineConfig();
        this.engineRegistry = new TemplateEngineRegistry();
    }

    /**
     * Initializes the template engines after configuration.
     */
    @PostConstruct
    public void postConstruct() {
        // Add global functions to the engine config
        for (Map.Entry<String, Object> entry : globalFunctions.entrySet()) {
            engineConfig.addGlobalVariable(entry.getKey(), entry.getValue());
        }

        // Initialize all engines with the config
        engineRegistry.initializeAll(engineConfig);
    }

    /**
     * Performs any necessary template configuration steps.
     * <p>
     * Intended to be overridden by subclasses providing
     * additional setup logic.
     */
    public abstract void configure();

    /**
     * Sets the directory for template loading.
     *
     * @param path The classpath prefix (e.g., "/templates")
     * @return This configuration instance for method chaining
     */
    public TemplateConfiguration setTemplateClasspath(String path) {
        engineConfig.setTemplatePath(path);
        return this;
    }

    /**
     * Sets whether template caching is enabled.
     *
     * @param enabled True to enable caching, false otherwise
     * @return This configuration instance for method chaining
     */
    public TemplateConfiguration setCaching(boolean enabled) {
        engineConfig.setCachingEnabled(enabled);
        return this;
    }

    /**
     * Sets the default encoding for templates.
     *
     * @param encoding The encoding to use
     * @return This configuration instance for method chaining
     */
    public TemplateConfiguration setDefaultEncoding(String encoding) {
        engineConfig.setDefaultEncoding(encoding);
        return this;
    }

    /**
     * Adds a global function/method that will be available in all templates.
     *
     * @param name The name to use for the function in templates
     * @param impl The function implementation object
     * @return This configuration instance for method chaining
     */
    public TemplateConfiguration addGlobalFunction(String name, Object impl) {
        globalFunctions.put(name, impl);
        engineConfig.addGlobalVariable(name, impl);
        return this;
    }

    /**
     * Gets the map of global functions registered with this configuration.
     *
     * @return The map of global function names to their implementations
     */
    public Map<String, Object> getGlobalFunctions() {
        return new HashMap<>(globalFunctions);
    }

    /**
     * Sets the default template engine to use.
     *
     * @param engineName The name of the engine to use as default
     * @return This configuration instance for method chaining
     */
    public TemplateConfiguration setDefaultEngine(String engineName) {
        engineRegistry.setDefaultEngine(engineName);
        return this;
    }

    /**
     * Registers a custom template engine.
     *
     * @param engine The template engine to register
     * @return This configuration instance for method chaining
     */
    public TemplateConfiguration registerEngine(TemplateEngine engine) {
        engineRegistry.register(engine);
        return this;
    }

    /**
     * Gets the default template engine.
     *
     * @return The default template engine
     */
    public TemplateEngine getDefaultEngine() {
        return engineRegistry.getDefaultEngine();
    }

    /**
     * Gets a template engine by name.
     *
     * @param engineName The name of the engine to get
     * @return The template engine
     */
    public TemplateEngine getEngine(String engineName) {
        return engineRegistry.getEngine(engineName);
    }

    /**
     * Resets the configuration to default settings.
     */
    public void reset() {
        globalFunctions.clear();
        configure();
    }
}