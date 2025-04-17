package io.github.t1willi.template;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import freemarker.core.XHTMLOutputFormat;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import io.github.t1willi.exceptions.TemplatingException;
import io.github.t1willi.security.nonce.NonceDirective;
import lombok.Getter;

/**
 * Defines an abstract configuration mechanism for FreeMarker templates.
 * Provides capabilities to configure the FreeMarker engine and template loader.
 * ie: loading paths, caching settings, etc.
 * <p>
 * 
 * Subclasses should implement {@link #configure()} to perform the
 * configuration.
 * 
 * @author William Beaudin.
 * @since 2.0
 */
public abstract class TemplateConfiguration {

    /**
     * The underlying FreeMarker configuration.
     */
    @Getter
    protected final Configuration configuration;

    /**
     * Map of custom Freemarker functions/methods to be added to templates.
     */
    protected final Map<String, Object> globalFunctions = new HashMap<>();

    public TemplateConfiguration() {
        this.configuration = new Configuration(Configuration.VERSION_2_3_32);
        this.configuration.setDefaultEncoding("UTF-8");
        this.configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        this.configuration.setIncompatibleImprovements(Configuration.VERSION_2_3_32);
        this.configuration.setOutputFormat(XHTMLOutputFormat.INSTANCE);
        this.configuration.setLogTemplateExceptions(true);
        this.configuration.setWrapUncheckedExceptions(true);
        this.configuration.setSharedVariable("nonce", new NonceDirective());
        this.configure();
    }

    /**
     * Performs any necessary Freemarker configuration steps.
     * <p>
     * Intended to be overridden by subclasses providing
     * additional setup logic.
     */
    public abstract void configure();

    /**
     * Sets the directory for template loading.
     *
     * @param directory The directory containing templates
     * @return This configuration instance for method chaining
     * @throws IOException If the directory cannot be accessed
     */
    public TemplateConfiguration setTemplateDirectory(String directory) throws IOException {
        try {
            configuration.setDirectoryForTemplateLoading(new File(directory));
        } catch (IOException e) {
            throw new TemplatingException("Failed to set template directory", e);
        }
        return this;
    }

    /**
     * Sets the class loader for template loading.
     * Useful for loading templates from classpath resources.
     *
     * @param prefix The classpath prefix (e.g., "/templates")
     * @param clazz  The class whose class loader to use
     * @return This configuration instance for method chaining
     */
    public TemplateConfiguration setTemplateClasspath(String prefix) {
        configuration.setClassLoaderForTemplateLoading(this.getClass().getClassLoader(), prefix);
        return this;
    }

    /**
     * Sets whether template caching is enabled.
     *
     * @param enabled True to enable caching, false otherwise
     * @return This configuration instance for method chaining
     */
    public TemplateConfiguration setCaching(boolean enabled) {
        configuration.setTemplateUpdateDelayMilliseconds(enabled ? 3600000 : 0);
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
     * Resets the configuration to default settings.
     */
    public void reset() {
        globalFunctions.clear();
        configure();
    }
}
