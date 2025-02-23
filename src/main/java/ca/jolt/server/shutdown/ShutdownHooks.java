package ca.jolt.server.shutdown;

import ca.jolt.exceptions.ServerException;
import ca.jolt.server.abstraction.WebServer;

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
