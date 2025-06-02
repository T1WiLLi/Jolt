package io.github.t1willi.template;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import lombok.Getter;

/**
 * Defines the configuration mechanism for template engines in the Jolt
 * framework.
 * <p>
 * This class allows configuring a single template engine instead of multiple
 * engines.
 * 
 * @author William Beaudin.
 * @since 3.0
 */
public abstract class TemplateConfiguration {
    private static final Logger LOGGER = Logger.getLogger(TemplateConfiguration.class.getName());

    /**
     * The configuration for template engines.
     */
    @Getter
    protected final TemplateEngineConfig engineConfig;

    /**
     * The template engine instance.
     */
    private TemplateEngine engine;

    /**
     * Map of custom functions/methods to be added to templates.
     */
    protected final Map<String, Object> globalFunctions = new HashMap<>();

    /**
     * Creates a new template configuration.
     */
    public TemplateConfiguration() {
        this.engineConfig = new TemplateEngineConfig();
        configure();

        for (Map.Entry<String, Object> entry : globalFunctions.entrySet()) {
            engineConfig.addGlobalVariable(entry.getKey(), entry.getValue());
        }

        if (engine != null && !engine.isInitialized()) {
            this.engine.initialize(engineConfig.copy());
        } else {
            LOGGER.severe("No template engine set!");
        }
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
     * Sets the template engine.
     *
     * @param engine The template engine to use
     * @return This configuration instance for method chaining
     */
    public TemplateConfiguration setEngine(TemplateEngine engine) {
        this.engine = engine;
        return this;
    }

    /**
     * Sets the template engine by class.
     *
     * @param engineClass The class of the template engine to use
     * @return This configuration instance for method chaining
     * @throws IllegalArgumentException If the engine couldn't be instantiated
     */
    public TemplateConfiguration setEngine(Class<? extends TemplateEngine> engineClass) {
        try {
            this.engine = engineClass.getDeclaredConstructor().newInstance();
            return this;
        } catch (Exception e) {
            LOGGER.severe("Failed to instantiate template engine: " + engineClass.getName());
            throw new IllegalArgumentException("Failed to instantiate template engine", e);
        }
    }

    /**
     * Gets the template engine.
     *
     * @return The template engine
     * @throws IllegalStateException If no engine is set
     */
    public TemplateEngine getEngine() {
        if (engine == null) {
            throw new IllegalStateException("No template engine set");
        }
        return engine;
    }

    /**
     * Resets the configuration to default settings.
     */
    public void reset() {
        globalFunctions.clear();
        engine = null;
        configure();
    }
}