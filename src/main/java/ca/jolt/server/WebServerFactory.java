package ca.jolt.server;

import ca.jolt.exceptions.ServerException;
import ca.jolt.server.abstraction.WebServer;
import ca.jolt.server.config.ServerConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class WebServerFactory {
    private static final Map<WebServerType, Function<ServerConfig, WebServer>> registeredServers = new HashMap<>();

    static {
        registerServer(WebServerType.TOMCAT, TomcatServer::new);
    }

    public static void registerServer(WebServerType type, Function<ServerConfig, WebServer> creator) {
        registeredServers.put(type, creator);
    }

    public static WebServer createServer(WebServerType type, ServerConfig config) throws ServerException {
        Function<ServerConfig, WebServer> creator = registeredServers.get(type);
        if (creator == null) {
            throw new ServerException("No registered implementation for server type: " + type);
        }
        return creator.apply(config);
    }
}
