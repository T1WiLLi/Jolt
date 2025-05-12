package io.github.t1willi.template;

import io.github.t1willi.exceptions.JoltHttpException;

/**
 * Core interface for template rendering engines in the Jolt framework.
 * <p>
 * This interface abstracts the templationg functionality, allowing different
 * template engine to be used interchangeably.
 * The framework provides implementations for common tomplate engine like
 * Freemarker and Thymeleaf out of the box.
 * 
 * @author William Beaudin
 * @since 2.6.6
 */
public interface TemplateEngine {

    /**
     * Renders a template with the provided model data.
     * 
     * @param templateName The name/path of the template to render.
     * @param model        The data model to use for rendering
     * @return The rendered template as a string.
     * @throws JoltHttpException If an error occurs during rendering.
     */
    String render(String templateName, JoltModel model) throws JoltHttpException;

    /**
     * Initializes the template engine with configuration settings.
     * <p>
     * This method should be called during application startup.
     * 
     * @param config The configuration object for this engine
     */
    void initialize(TemplateEngineConfig config);

    /**
     * Gets the name of this template engine implementation.
     * 
     * @return The name of the template engine
     */
    String getName();

    /**
     * Gets the file extension used by this template engine.
     * <p>
     * For example, ".ftl" for FreeMarker or ".html" for Thymeleaf.
     * 
     * @return The file extension including the dot
     */
    String getFileExtension();

    /**
     * Checks if the template engine has been initialized.
     * 
     * @return true if initialized, false otherwise
     */
    boolean isInitialized();
}
