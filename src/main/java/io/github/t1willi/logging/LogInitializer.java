package io.github.t1willi.logging;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Automatically initializes the Jolt logging system when the servlet context is
 * created.
 * This listener applies Jolt's custom logging format to both application and
 * Tomcat logs.
 * 
 * <p>
 * The initialization happens automatically - no additional configuration is
 * required.
 * The listener is registered through the {@code @WebListener} annotation.
 * 
 * <p>
 * To disable automatic initialization, you can exclude this class from
 * component scanning
 * or override the logging configuration in your web.xml.
 * 
 * @author William Beaudin
 * @see LogConfigurator
 * @see LogFormatter
 * @since 1.0
 */
@WebListener
public class LogInitializer implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LogConfigurator.configure();
    }
}
