package ca.jolt.server;

import ca.jolt.core.JoltDispatcherServlet;
import ca.jolt.exceptions.ServerException;
import ca.jolt.server.config.ServerConfig;
import jakarta.servlet.MultipartConfigElement;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.startup.Tomcat;

import java.io.File;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.logging.Logger;
import java.util.stream.Stream;

public final class TomcatServer {
    private static final Logger log = Logger.getLogger(TomcatServer.class.getName());

    private final ServerConfig config;
    private Tomcat tomcat;

    public TomcatServer(ServerConfig config) {
        this.config = config;
    }

    public void start() throws ServerException {
        try {
            validateEnvironment();
            initializeTomcat();
            Connector connector = createAndConfigureConnector();
            StandardThreadExecutor executor = createAndConfigureExecutor();
            tomcat.getService().addExecutor(executor);
            tomcat.setConnector(connector);
            configureContext();
            startTomcat();
            logServerStart();
            addShutdownHook();
            handleDaemonMode();
        } catch (Exception e) {
            throw new ServerException("Failed to start Tomcat: " + e.getMessage(), e);
        }
    }

    public void stop() throws ServerException {
        try {
            if (tomcat != null) {
                tomcat.stop();
                deleteTempDir();
                log.info("Tomcat stopped successfully");
            }
        } catch (Exception e) {
            throw new ServerException("Failed to stop Tomcat", e);
        }
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
            } catch (ServerException e) {
                log.severe(() -> "Failed to stop Tomcat on shutdown: " + e.getMessage());
            }
        }));
    }

    private void validateEnvironment() throws ServerException {
        if (!isPortAvailable(config.getPort())) {
            throw new ServerException("Port " + config.getPort() + " is already in use");
        }
        ensureTempDirExists();
    }

    private void initializeTomcat() {
        tomcat = new Tomcat();
        tomcat.setPort(config.getPort());
        String absoluteTempDir = new File(config.getTempDir()).getAbsolutePath();
        tomcat.setBaseDir(absoluteTempDir);
    }

    private Connector createAndConfigureConnector() throws ServerException {
        Connector connector = new Connector();
        connector.setPort(config.getPort());
        connector.setURIEncoding("UTF-8");
        if (config.isSslEnabled()) {
            validateSslConfig();
            connector.setSecure(true);
            connector.setScheme("https");
            connector.setPort(config.getSslPort());
            connector.setProperty("keystoreFile", config.getKeyStore());
            connector.setProperty("keystorePass", config.getKeyStorePassword());
            connector.setProperty("keyAlias", config.getKeyAlias());
            connector.setProperty("SSLEnabled", "true");
            connector.setProperty("sslProtocol", "TLS");
        }
        return connector;
    }

    private StandardThreadExecutor createAndConfigureExecutor() {
        StandardThreadExecutor executor = new StandardThreadExecutor();
        executor.setName("tomcatExecutor");
        executor.setMinSpareThreads(config.getThreadsMin());
        executor.setMaxThreads(config.getThreadsMax());
        executor.setMaxIdleTime((int) config.getThreadsTimeout());
        return executor;
    }

    private void configureContext() throws ServerException {
        String docBase = new File(config.getTempDir()).getAbsolutePath();
        try {
            Context context = tomcat.addContext("", docBase);

            context.setMapperContextRootRedirectEnabled(false);
            context.setMapperDirectoryRedirectEnabled(false);
            context.setAllowCasualMultipartParsing(false);
            context.setReloadable(false);

            Wrapper defaultServlet = Tomcat.addServlet(context, "default",
                    "org.apache.catalina.servlets.DefaultServlet");
            defaultServlet.addInitParameter("listings", "false");
            defaultServlet.setLoadOnStartup(1);
            context.addServletMappingDecoded("/", "default");

            MultipartConfigElement multipartConfig = new MultipartConfigElement(
                    new File(config.getTempDir()).getAbsolutePath(),
                    config.getMultipartMaxFileSize(),
                    config.getMultipartMaxRequestSize(),
                    config.getMultipartFileSizeThreshold());

            JoltDispatcherServlet dispatcher = new JoltDispatcherServlet();
            Wrapper servletWrapper = Tomcat.addServlet(context, "JoltServlet", dispatcher);

            servletWrapper.setMultipartConfigElement(multipartConfig);
            context.addServletMappingDecoded("/*", "JoltServlet");
        } catch (Exception e) {
            throw new ServerException("Failed to configure context", e);
        }
    }

    private void startTomcat() throws LifecycleException {
        tomcat.start();
    }

    private void logServerStart() {
        String message = config.getAppName() + " started on port " + config.getPort();
        if (config.isSslEnabled()) {
            message += " (SSL on port " + config.getSslPort() + ")";
        }
        log.info(message);
    }

    private void handleDaemonMode() {
        if (!config.isDaemon()) {
            tomcat.getServer().await();
        }
    }

    private boolean isPortAvailable(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
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

    private void deleteTempDir() {
        try {
            Path tomcatPath = Paths.get(config.getTempDir());
            if (Files.exists(tomcatPath)) {
                try (Stream<Path> paths = Files.walk(tomcatPath)) {
                    paths.sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (Exception e) {
                                    log.warning("Failed to delete path: " + path + " - " + e.getMessage());
                                }
                            });
                }
            }
        } catch (Exception e) {
            log.warning("Failed to delete temp directory structure: " + e.getMessage());
        }
    }

    private void validateSslConfig() throws ServerException {
        if (config.getKeyStore() == null || config.getKeyStore().isEmpty()) {
            log.severe("SSL is enabled but no keystore file is specified (server.ssl.keyStore)");
            throw new ServerException("SSL configuration error: keystore file is required");
        }
        File keyStoreFile = new File(config.getKeyStore());
        if (!keyStoreFile.exists() || !keyStoreFile.isFile()) {
            log.severe("SSL is enabled but the keystore file does not exist: " + config.getKeyStore());
            throw new ServerException("SSL configuration error: keystore file does not exist: " + config.getKeyStore());
        }
        if (config.getKeyStorePassword() == null || config.getKeyStorePassword().isEmpty()) {
            log.severe("SSL is enabled but no keystore password is specified (server.ssl.keyStorePassword)");
            throw new ServerException("SSL configuration error: keystore password is required");
        }
        if (config.getKeyAlias() == null || config.getKeyAlias().isEmpty()) {
            log.severe("SSL is enabled but no key alias is specified (server.ssl.keyAlias)");
            throw new ServerException("SSL configuration error: key alias is required");
        }
    }
}