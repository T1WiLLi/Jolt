package ca.jolt.tomcat;

import ca.jolt.tomcat.abstraction.WebServer;
import ca.jolt.tomcat.config.ServerConfig;
import ca.jolt.tomcat.config.SslConfig;
import ca.jolt.exceptions.ServerException;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.net.ServerSocket;

public class TomcatServer implements WebServer {

    private static final Logger logger = Logger.getLogger(TomcatServer.class.getName());
    private static final String DEFAULT_CONTEXT_PATH = "/";

    private final Tomcat tomcat;
    private final ServerConfig config;

    protected TomcatServer(ServerConfig config) {
        this.tomcat = new Tomcat();
        this.config = config != null ? config : new ServerConfig();
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
            logger.info("Server started successfully on port " + config.getPort());
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

    @Override
    public void restart() throws ServerException {
        stop();
        start();
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void ensureTempDirExists() throws ServerException {
        try {
            Path tempDir = Paths.get(config.getTempDir());
            Files.createDirectories(tempDir);
        } catch (Exception e) {
            throw new ServerException("Failed to create temp directory: " + config.getTempDir(), e);
        }
    }

    private void configureTomcat() throws ServerException {
        try {
            configureTempDir();
            configureConnector();
            configureContext();

            if (config.getSsl().isEnabled()) {
                configureSslConnector();
            }
        } catch (Exception e) {
            throw new ServerException("Failed to configure Tomcat", e);
        }
    }

    private void configureTempDir() {
        tomcat.setBaseDir(config.getTempDir());
    }

    private void deleteTempDir() {
        try {
            Path tempPath = Paths.get(config.getTempDir());
            Files.walk(tempPath)
                    .sorted((a, b) -> b.compareTo(a))
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            logger.warning("Failed to delete temp directory: " + e.getMessage());
        }
    }

    private void configureConnector() {
        Connector connector = new Connector();
        connector.setPort(config.getPort());
        connector.setProperty("maxThreads", String.valueOf(config.getThreads().getMaxThreads()));
        connector.setProperty("minSpareThreads", String.valueOf(config.getThreads().getMinThreads()));
        connector.setProperty("connectionTimeout", String.valueOf(config.getThreads().getTimeout()));
        tomcat.setConnector(connector);
    }

    private void configureSslConnector() throws ServerException {
        SslConfig ssl = config.getSsl();
        if (!isPortAvailable(ssl.getPort())) {
            throw new ServerException("SSL Port " + ssl.getPort() + " is already in use");
        }

        if (ssl.getKeystorePath() == null || ssl.getKeystorePassword() == null) {
            throw new ServerException("SSL configuration is incomplete");
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

    private void configureContext() throws ServerException {
        String docBase = new File(config.getTempDir()).getAbsolutePath();

        try {
            Context context = tomcat.addContext(DEFAULT_CONTEXT_PATH, docBase);
            context.setReloadable(false);

            Tomcat.addServlet(context, "default", "org.apache.catalina.servlets.DefaultServlet");
            context.addServletMappingDecoded("/", "default");

        } catch (Exception e) {
            throw new ServerException("Failed to configure context", e);
        }
    }
}