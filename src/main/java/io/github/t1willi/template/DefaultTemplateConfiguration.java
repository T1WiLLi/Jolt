package io.github.t1willi.template;

import io.github.t1willi.injector.annotation.Configuration;
import io.github.t1willi.injector.type.ConfigurationType;
import jakarta.annotation.PostConstruct;

/**
 * Provides the default Freemarker configuration for the Jolt framework.
 * <p>
 * This class is annotated with {@link JoltConfiguration} to indicate that it
 * serves as the primary Freemarker configuration. Applications can override the
 * default behavior by defining their own configuration class with
 * {@code @JoltConfiguration(value = ConfigurationType.TEMPLATING)}.
 * <p>
 * The {@link #configure()} method is called during Freemarker setup and can be
 * overridden in custom implementations to provide additional logic.
 *
 * @author William Beaudin.
 * @since 2.0
 */
@Configuration(value = ConfigurationType.TEMPLATE, isDefault = true)
public final class DefaultTemplateConfiguration extends TemplateConfiguration {

    @PostConstruct
    public void init() {
        configure();
    }

    /**
     * Performs Freemarker configuration steps for the default setup.
     * <p>
     * The default implementation sets up templates to be loaded from
     * the "templates" directory in the resources folder.
     */
    @Override
    public void configure() {
        setTemplateClasspath("/templates");
        setCaching(true);
        addGlobalFunction("format", new TemplateFormatters());
    }
}
