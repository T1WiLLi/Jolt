package io.github.t1willi.template;

import io.github.t1willi.injector.annotation.Configuration;
import io.github.t1willi.injector.type.ConfigurationType;

/**
 * Provides the default template configuration for the Jolt framework.
 * <p>
 * This class is annotated with {@link Configuration} to indicate that it
 * serves as the primary template configuration. Applications can override the
 * default behavior by defining their own configuration class with
 * {@code @Configuration(value = ConfigurationType.TEMPLATE)}.
 * <p>
 * The {@link #configure()} method is called during setup and can be
 * overridden in custom implementations to provide additional logic.
 *
 * @author William Beaudin.
 * @since 2.0
 */
@Configuration(value = ConfigurationType.TEMPLATE, isDefault = true)
public final class DefaultTemplateConfiguration extends TemplateConfiguration {

    /**
     * Performs template configuration steps for the default setup.
     * <p>
     * The default implementation sets up templates to be loaded from
     * the "templates" directory in the resources folder, enables caching,
     * and adds the standard formatting functions.
     */
    @Override
    public void configure() {
        setTemplateClasspath("/templates");
        setCaching(true);
        addGlobalFunction("format", new TemplateFormatters());
        // Use FreeMarker as the default engine (already set in registry)
    }
}