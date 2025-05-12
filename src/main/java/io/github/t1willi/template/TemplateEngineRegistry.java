package io.github.t1willi.template;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import io.github.t1willi.template.engines.FreemarkerTemplateEngine;

/**
 * Registry for template engines in the Jolt framework.
 * <p>
 * This class manages the available template engines and provides
 * access to them. It allows registering custom template engines
 * and retrieving engines by name.
 * 
 * @author William Beaudin
 * @since 2.1
 */
public class TemplateEngineRegistry {

    private static final Logger LOGGER = Logger.getLogger(TemplateEngineRegistry.class.getName());

    private final Map<String, TemplateEngine> engines = new HashMap<>();
    private String defaultEngineName;

    /**
     * Creates a new template engine registry with default engines.
     */
    public TemplateEngineRegistry() {
        // Register default engines
        registerDefaultEngines();
    }

    /**
     * Registers the default template engines.
     */
    private void registerDefaultEngines() {
        FreemarkerTemplateEngine freemarkerEngine = new FreemarkerTemplateEngine();
        register(freemarkerEngine);

        setDefaultEngine(freemarkerEngine.getName());

        try {
            Class.forName("org.thymeleaf.TemplateEngine");
            Object thymeleafEngine = Class.forName("io.github.t1willi.template.engines.ThymeleafTemplateEngine")
                    .getDeclaredConstructor().newInstance();
            if (thymeleafEngine instanceof TemplateEngine) {
                register((TemplateEngine) thymeleafEngine);
            }
        } catch (Exception e) {
            LOGGER.fine("Thymeleaf not available on classpath, skipping registration");
        }
    }

    /**
     * Registers a template engine.
     *
     * @param engine The template engine to register
     * @return This registry instance for method chaining
     */
    public TemplateEngineRegistry register(TemplateEngine engine) {
        engines.put(engine.getName().toLowerCase(), engine);
        return this;
    }

    /**
     * Sets the default template engine.
     *
     * @param engineName The name of the engine to set as default
     * @return This registry instance for method chaining
     * @throws IllegalArgumentException If no engine with the given name is
     *                                  registered
     */
    public TemplateEngineRegistry setDefaultEngine(String engineName) {
        String key = engineName.toLowerCase();
        if (!engines.containsKey(key)) {
            throw new IllegalArgumentException("No template engine registered with name: " + engineName);
        }
        this.defaultEngineName = key;
        return this;
    }

    /**
     * Gets a template engine by name.
     *
     * @param engineName The name of the engine to get
     * @return The template engine
     * @throws IllegalArgumentException If no engine with the given name is
     *                                  registered
     */
    public TemplateEngine getEngine(String engineName) {
        String key = engineName.toLowerCase();
        if (!engines.containsKey(key)) {
            throw new IllegalArgumentException("No template engine registered with name: " + engineName);
        }
        return engines.get(key);
    }

    /**
     * Gets the default template engine.
     *
     * @return The default template engine
     */
    public TemplateEngine getDefaultEngine() {
        return engines.get(defaultEngineName);
    }

    /**
     * Gets the names of all registered template engines.
     *
     * @return A set of engine names
     */
    public Set<String> getRegisteredEngineNames() {
        return engines.keySet();
    }

    /**
     * Initializes all registered template engines with the provided configuration.
     *
     * @param config The configuration to use
     */
    public void initializeAll(TemplateEngineConfig config) {
        for (TemplateEngine engine : engines.values()) {
            if (!engine.isInitialized()) {
                engine.initialize(config.copy());
            }
        }
    }
}