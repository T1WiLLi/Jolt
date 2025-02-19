package ca.jolt.tomcat.abstraction;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.logging.Logger;

import ca.jolt.exceptions.ServerException;
import ca.jolt.tomcat.config.ServerConfig;

public abstract class AbstractWebServer implements WebServer {
    protected ServerConfig config;
    protected final Logger logger = Logger.getLogger(getClass().getName());

    @Override
    public void configure(ServerConfig config) {
        this.config = config;
    }

    @Override
    public void restart() throws ServerException {
        stop();
        start();
    }

    protected boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected void ensureTempDirExists() throws ServerException {
        try {
            Path tempDir = Paths.get(config.getTempDir());
            Files.createDirectories(tempDir);
        } catch (Exception e) {
            throw new ServerException("Failed to create temp directory: " + config.getTempDir(), e);
        }
    }

    protected void deleteTempDir() {
        try {
            Path tomcatPath = Paths.get(config.getTempDir());
            if (Files.exists(tomcatPath)) {
                Files.walk(tomcatPath)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                logger.warning("Failed to delete path: " + path + " - " + e.getMessage());
                            }
                        });
            }

            Path tmpPath = tomcatPath.getParent();
            if (tmpPath != null && Files.exists(tmpPath)) {
                try {
                    Files.delete(tmpPath);
                } catch (IOException e) {
                    logger.warning("Failed to delete tmp directory: " + tmpPath + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.warning("Failed to delete temp directory structure: " + e.getMessage());
        }
    }
}
