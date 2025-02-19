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

        // Exemple of how to setup route with Jolt
        // Jolt.get("/", () -> "Hello World!"); // This is the most basic route.
        // Jolt.get("/hello", (context) -> "Hello" + context.query("name")); // This
        // route has a query parameter.
        // Jolt.get("/hello/{name}", (context) -> "Hello " + context.path("name")); //
        // This route has a path parameter.
        // Jolt.get("/template", (context) -> new Template("index.html", new
        // Model().withAttribute("name", "World"))); // This route uses a template.
        // Jolt.get("/json", (context) -> new Person("Bob Dole", 18)).asJson(); // This
        // route returns a JSON response.
    }
}