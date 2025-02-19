package ca.jolt.tomcat.shutdown;

import ca.jolt.exceptions.ServerException;
import ca.jolt.tomcat.abstraction.WebServer;

public class ShutdownHooks {
    public static void addShutdownHook(WebServer server) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
            } catch (ServerException e) {
                e.printStackTrace();
            }
        }));
    }
}
