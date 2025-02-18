package ca.jolt;

import java.util.logging.Logger;

import ca.jolt.logging.LogConfigurator;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        LogConfigurator.configure();

        ServerConfig serverConfig = new ServerConfig();
        TomcatServer server = new TomcatServer(serverConfig);
        logger.info("Tomcat started. Visit http://localhost:8080 to test.");
        server.start();
    }
}