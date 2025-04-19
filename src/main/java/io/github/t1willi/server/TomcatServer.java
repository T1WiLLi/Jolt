package io.github.t1willi.server;

import jakarta.servlet.MultipartConfigElement;
import lombok.Getter;

import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;

import io.github.t1willi.core.JoltDispatcherServlet;
import io.github.t1willi.exceptions.ServerException;
import io.github.t1willi.security.session.SessionManager;
import io.github.t1willi.server.config.ServerConfig;

import java.io.File;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class TomcatServer {
    private static final Logger log = Logger.getLogger(TomcatServer.class.getName());
    private final ServerConfig config;
    private Tomcat tomcat;
    @Getter
    private Context context;

    public TomcatServer(ServerConfig config) {
        this.config = config;
    }

    public void start() throws ServerException {
        try {
            validateEnvironment();
            initializeTomcat();
            tomcat.getService().addExecutor(createAndConfigureExecutor());
            for (Connector connector : createAndConfigureConnectors()) {
                tomcat.getService().addConnector(connector);
            }
            configureContext();
            SessionManager.initialize(this);
            tomcat.start();
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
        if (config.isHttpEnabled() && !isPortAvailable(config.getPort())) {
            throw new ServerException("Port " + config.getPort() + " is already in use");
        }
        if (config.isSslEnabled() && !isPortAvailable(config.getSslPort())) {
            throw new ServerException("Port " + config.getSslPort() + " is already in use");
        }
    }

    private void initializeTomcat() {
        tomcat = new Tomcat();
        tomcat.setPort(config.isHttpEnabled() ? config.getPort() : config.getSslPort());
        tomcat.setBaseDir(new File(config.getTempDir()).getAbsolutePath());
    }

    private Connector[] createAndConfigureConnectors() throws ServerException {
        List<Connector> connectors = new ArrayList<>();

        if (config.isHttpEnabled()) {
            Connector httpConnector = new Connector("org.apache.coyote.http11.Http11Nio2Protocol");
            httpConnector.setPort(config.getPort());
            httpConnector.setURIEncoding("UTF-8");
            httpConnector.setProperty("compression", "on");
            httpConnector.setProperty("compressionMinSize", "1024");
            httpConnector.setProperty("compressionMethod", "gzip");
            httpConnector.setProperty("compressableMimeType",
                    "text/html,text/xml,text/css,text/javascript,application/javascript,application/json");
            httpConnector.addUpgradeProtocol(new Http2Protocol()); // Support HTTP/2
            connectors.add(httpConnector);
        }

        if (config.isSslEnabled()) {
            validateSslConfig();
            Connector httpsConnector = new Connector("org.apache.coyote.http11.Http11Nio2Protocol");
            httpsConnector.setSecure(true);
            httpsConnector.setScheme("https");
            httpsConnector.setPort(config.getSslPort());

            SSLHostConfig sslHostConfig = new SSLHostConfig();
            sslHostConfig.setHostName("_default_");
            sslHostConfig.setSslProtocol("TLS");
            sslHostConfig.setProtocols("TLSv1.3,TLSv1.2");

            SSLHostConfigCertificate certificate = new SSLHostConfigCertificate(
                    sslHostConfig, SSLHostConfigCertificate.Type.UNDEFINED);
            certificate.setCertificateKeystoreFile(config.getKeyStore());
            certificate.setCertificateKeystorePassword(config.getKeyStorePassword());
            certificate.setCertificateKeyAlias(config.getKeyAlias());

            sslHostConfig.addCertificate(certificate);
            httpsConnector.addSslHostConfig(sslHostConfig);

            httpsConnector.setProperty("SSLEnabled", "true");
            httpsConnector.addUpgradeProtocol(new Http2Protocol());
            connectors.add(httpsConnector);
        }
        if (connectors.isEmpty()) {
            throw new ServerException("No connectors configured. At least one of HTTP or HTTPS must be enabled.");
        }

        return connectors.toArray(new Connector[0]);
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
        try {
            context = tomcat.addContext("", new File(config.getTempDir()).getAbsolutePath());

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
                    (String) context.getServletContext().getAttribute("javax.servlet.context.tempdir"),
                    config.getMultipartMaxFileSize(),
                    config.getMultipartMaxRequestSize(),
                    config.getMultipartFileSizeThreshold());

            JoltDispatcherServlet dispatcher = new JoltDispatcherServlet();
            Wrapper servletWrapper = Tomcat.addServlet(context, "JoltServlet", dispatcher);
            servletWrapper.setMultipartConfigElement(multipartConfig);
            context.addServletMappingDecoded("/*", "JoltServlet");
        } catch (Exception e) {
            throw new ServerException("Failed to configure context " + e.getMessage(), e);
        }
    }

    private void logServerStart() {
        String message = config.getAppName() + " started on port "
                + (config.isHttpEnabled() ? config.getPort() : config.getSslPort());
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

    private void validateSslConfig() throws ServerException {
        if (config.getKeyStore() == null || config.getKeyStore().isEmpty()) {
            throw new ServerException("SSL configuration error: keystore file is required");
        }
        File keyStoreFile = new File(config.getKeyStore());
        if (!keyStoreFile.exists() || !keyStoreFile.isFile()) {
            throw new ServerException("SSL configuration error: keystore file does not exist: " + config.getKeyStore());
        }
        if (config.getKeyStorePassword() == null || config.getKeyStorePassword().isEmpty()) {
            throw new ServerException("SSL configuration error: keystore password is required");
        }
        if (config.getKeyAlias() == null || config.getKeyAlias().isEmpty()) {
            throw new ServerException("SSL configuration error: key alias is required");
        }
    }
}
