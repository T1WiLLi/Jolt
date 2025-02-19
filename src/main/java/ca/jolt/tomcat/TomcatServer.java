package ca.jolt.tomcat;

import ca.jolt.exceptions.ServerException;
import ca.jolt.tomcat.abstraction.AbstractWebServer;
import ca.jolt.tomcat.config.ServerConfig;
import ca.jolt.tomcat.config.SslConfig;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

import java.io.File;

public class TomcatServer extends AbstractWebServer {
    private Tomcat tomcat;

    public TomcatServer(ServerConfig config) {
        configure(config);
    }

    @Override
    public void start() throws ServerException {
        try {
            if (!isPortAvailable(config.getPort())) {
                throw new ServerException("Port " + config.getPort() + " is already in use");
            }

            ensureTempDirExists();
            configureTomcat();
            tomcat.start();
            logger.info("Tomcat started on port " + config.getPort());
        } catch (Exception e) {
            throw new ServerException("Failed to start the server", e);
        }
    }

    @Override
    public void stop() throws ServerException {
        try {
            tomcat.stop();
            deleteTempDir();
            logger.info("Server stopped successfully");
        } catch (Exception e) {
            throw new ServerException("Failed to stop the server", e);
        }
    }

    private void configureTomcat() throws ServerException {
        tomcat = new Tomcat();
        tomcat.setBaseDir(config.getTempDir());

        Connector connector = new Connector();
        connector.setPort(config.getPort());
        tomcat.setConnector(connector);

        configureContext();

        if (config.getSsl().isEnabled()) {
            configureSslConnector();
        }
    }

    private void configureContext() throws ServerException {
        String docBase = new File(config.getTempDir()).getAbsolutePath();
        try {
            Context context = tomcat.addContext("/", docBase);
            Tomcat.addServlet(context, "default", "org.apache.catalina.servlets.DefaultServlet");
            context.addServletMappingDecoded("/", "default");
        } catch (Exception e) {
            throw new ServerException("Failed to configure context", e);
        }
    }

    private void configureSslConnector() throws ServerException {
        SslConfig ssl = config.getSsl();
        Connector connector = new Connector();
        connector.setPort(ssl.getPort());
        connector.setSecure(true);
        connector.setScheme("https");
        connector.setProperty("SSLEnabled", "true");
        tomcat.getService().addConnector(connector);
    }
}