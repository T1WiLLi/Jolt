package ca.jolt.tomcat;

import ca.jolt.core.JoltDispatcherServlet;
import ca.jolt.core.Router;
import ca.jolt.exceptions.ServerException;
import ca.jolt.tomcat.abstraction.AbstractWebServer;
import ca.jolt.tomcat.config.ServerConfig;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

import java.io.File;
import java.util.logging.Logger;

public class TomcatServer extends AbstractWebServer {

    private static final Logger log = Logger.getLogger(TomcatServer.class.getName());

    private Tomcat tomcat;
    private Router router;
    private boolean customNotFound;
    private boolean customError;

    public TomcatServer(ServerConfig config) {
        configure(config);
    }

    @Override
    public void setRouter(Router router) {
        this.router = router;
    }

    public void setCustomErrorPages(boolean customNotFound, boolean customError) {
        this.customNotFound = customNotFound;
        this.customError = customError;
    }

    @Override
    public void start() throws ServerException {
        try {
            if (!isPortAvailable(config.getPort())) {
                throw new ServerException("Port " + config.getPort() + " is already in use");
            }
            ensureTempDirExists();

            tomcat = new Tomcat();
            tomcat.setPort(config.getPort());
            tomcat.setBaseDir(config.getTempDir());

            Connector connector = new Connector();
            connector.setPort(config.getPort());
            tomcat.setConnector(connector);

            configureContext();
            tomcat.start();
            log.info("Tomcat started on port " + config.getPort());
        } catch (Exception e) {
            throw new ServerException("Failed to start the server", e);
        }
    }

    @Override
    public void stop() throws ServerException {
        try {
            tomcat.stop();
            deleteTempDir();
            log.info("Tomcat stopped successfully");
        } catch (Exception e) {
            throw new ServerException("Failed to stop the server", e);
        }
    }

    private void configureContext() throws ServerException {
        String docBase = new File(config.getTempDir()).getAbsolutePath();
        try {
            Context context = tomcat.addContext("", docBase);

            if (router != null) {
                JoltDispatcherServlet dispatcher = new JoltDispatcherServlet(router, customNotFound, customError);
                Tomcat.addServlet(context, "JoltServlet", dispatcher);
                context.addServletMappingDecoded("/*", "JoltServlet");
            } else {
                log.warning("No router set for TomcatServer; no routes will be handled.");
            }
        } catch (Exception e) {
            throw new ServerException("Failed to configure context", e);
        }
    }
}