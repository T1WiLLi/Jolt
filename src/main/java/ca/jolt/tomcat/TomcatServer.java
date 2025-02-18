package ca.jolt.tomcat;

import ca.jolt.tomcat.abstraction.WebServer;
import ca.jolt.tomcat.config.ServerConfig;
import ca.jolt.tomcat.config.SslConfig;
import ca.jolt.exceptions.ServerException;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class TomcatServer implements WebServer {

    private static final Logger logger = Logger.getLogger(TomcatServer.class.getName());

    private final Tomcat tomcat;
    private final ServerConfig config;

    protected TomcatServer(ServerConfig config) {
        this.tomcat = new Tomcat();
        this.config = config != null ? config : new ServerConfig();
    }

    @Override
    public void start() throws ServerException {
        try {
            ensureTempDirExists();
            configureTomcat();
            tomcat.start();
        } catch (Exception e) {
            throw new ServerException("Failed to start the server", e);
        }
    }

    @Override
    public void stop() throws ServerException {
        try {
            tomcat.stop();
        } catch (Exception e) {
            throw new ServerException("Failed to stop the server", e);
        }
    }

    @Override
    public void restart() throws ServerException {
        stop();
        start();
    }

    private void ensureTempDirExists() {
        try {
            Path tempDir = Paths.get(config.getTempDir());
            Files.createDirectories(tempDir);
        } catch (Exception e) {
            logger.severe("Failed to create temp dir: " + e.getMessage());
        }
    }

    private void configureTomcat() {
        configureTempDir();
        configureConnector();
        configureContext();

        if (config.getSsl().isEnabled()) {
            configureSslConnector();
        }
    }

    private void configureTempDir() {
        tomcat.setBaseDir(config.getTempDir());
    }

    private void configureConnector() {
        Connector connector = new Connector();
        connector.setPort(config.getPort());
        tomcat.setConnector(connector);
    }

    private void configureSslConnector() {
        SslConfig ssl = config.getSsl();
        if (ssl.getKeystorePath() == null || ssl.getKeystorePassword() == null) {
            return;
        }

        Connector connector = new Connector();
        connector.setPort(ssl.getPort());
        connector.setSecure(true);
        connector.setScheme("https");
        connector.setProperty("SSLEnabled", "true");
        connector.setProperty("keystoreFile", ssl.getKeystorePath());
        connector.setProperty("keystorePass", ssl.getKeystorePassword());
        if (ssl.getKeyAlias() != null) {
            connector.setProperty("keyAlias", ssl.getKeyAlias());
        }
        tomcat.getService().addConnector(connector);
    }

    private void configureContext() {
        String contextPath = "/";
        String docBase = new File(config.getTempDir()).getAbsolutePath();
        Context context = tomcat.addContext(contextPath, docBase);
        context.setReloadable(false);
    }
}