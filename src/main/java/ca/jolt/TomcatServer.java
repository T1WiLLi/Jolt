package ca.jolt;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TomcatServer {
    private final Tomcat tomcat;
    private final ServerConfig config;
    private volatile ServerStatus status;

    public TomcatServer(ServerConfig config) {
        this.tomcat = new Tomcat();
        this.config = config != null ? config : new ServerConfig();
        this.status = ServerStatus.STOPPED;
    }

    public void start() {
        try {
            status = ServerStatus.STARTING;
            ensureTempDirExists();
            configureTomcat();
            tomcat.start();
            status = ServerStatus.RUNNING;
        } catch (Exception e) {
            status = ServerStatus.ERROR;
        }
    }

    private void ensureTempDirExists() {
        try {
            Path tempDir = Paths.get(config.getTempDir());
            Files.createDirectories(tempDir);
        } catch (Exception e) {
        }
    }

    public void stop() {
        try {
            status = ServerStatus.STOPPING;
            tomcat.stop();
            status = ServerStatus.STOPPED;
        } catch (Exception e) {
            status = ServerStatus.ERROR;
        }
    }

    public ServerStatus getStatus() {
        return status;
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