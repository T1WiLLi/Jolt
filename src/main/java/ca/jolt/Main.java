package ca.jolt;

import ca.jolt.logging.LogConfigurator;
import ca.jolt.logging.StartupLog;
import ca.jolt.tomcat.WebServerBuilder;
import ca.jolt.tomcat.abstraction.WebServer;

public class Main {
    public static void main(String[] args) throws Exception {
        StartupLog.printStartup(); // This will belong with JoltApplication class in the future
        LogConfigurator.configure(); // This will go into Hooks

        WebServer server = new WebServerBuilder().build();
        server.start();
    }
}