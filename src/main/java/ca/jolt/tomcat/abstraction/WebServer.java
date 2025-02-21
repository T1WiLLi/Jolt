package ca.jolt.tomcat.abstraction;

import ca.jolt.core.Router;
import ca.jolt.exceptions.ServerException;
import ca.jolt.exceptions.handler.GlobalExceptionHandler;
import ca.jolt.tomcat.config.ServerConfig;
import ca.jolt.tomcat.shutdown.ShutdownHooks;

public interface WebServer {
    public void start() throws ServerException;

    public void stop() throws ServerException;

    public void restart() throws ServerException;

    void configure(ServerConfig config) throws ServerException;

    void setRouter(Router router);

    void setExceptionHandler(GlobalExceptionHandler exceptionHandler);

    /**
     * Finalizes the build of the WebServer and adds a shutdown hook to stop the
     * server when the JVM is shutting down.
     *
     * @implNote This method can and should be overridden by the implementing class
     * @return the WebServer instance
     */
    default WebServer finalizeBuild() {
        ShutdownHooks.addShutdownHook(this);
        return this;
    }
}
