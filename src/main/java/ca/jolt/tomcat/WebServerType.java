package ca.jolt.tomcat;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents the supported types of servers.
 * The user can add new types dynamically.
 * 
 * <code>WebServerType jettyType = WebServerType.of("JETTY");
WebServerFactory.registerServer(jettyType, JettyServer::new);</code>
 */
public class WebServerType {
    private static final Map<String, WebServerType> TYPES = new HashMap<>();

    public static final WebServerType TOMCAT = new WebServerType("TOMCAT");

    private final String name;

    private WebServerType(String name) {
        this.name = name;
        TYPES.put(name, this);
    }

    public static WebServerType of(String name) {
        return TYPES.computeIfAbsent(name, WebServerType::new);
    }

    @Override
    public String toString() {
        return name;
    }
}