package ca.jolt;

import java.util.logging.Logger;

import ca.jolt.logging.LogConfigurator;
import ca.jolt.logging.StartupLog;
import ca.jolt.tomcat.WebServerBuilder;
import ca.jolt.tomcat.abstraction.WebServer;

public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        StartupLog.printStartup();
        LogConfigurator.configure();

        WebServer server = new WebServerBuilder()
                .withPort(8080)
                .withTempDir("tmp/tomcat")
                .build();

        logger.info("Tomcat started. Visit http://localhost:8080 to test.");
        server.start();
    }
}