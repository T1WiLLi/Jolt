package ca.jolt;

import ca.jolt.core.JoltApplication;
import ca.jolt.core.Router;
import ca.jolt.server.abstraction.WebServer;

public class Main extends JoltApplication {
    public static void main(String[] args) throws Exception {
        launch(Main.class, args);
    }

    @Override
    protected void setup() {
        buildServer()
                .withPort(8080);

        get("/", () -> "Hello, World!");
        get("/hello/{age:int}", (ctx) -> ctx.html("Hello " + ctx.query("name").orDefault("little one") + ", you are "
                + ctx.path("age").asInt() + " years old!"));
        post("/user", (ctx) -> {
            User user = ctx.body(User.class);
            return ctx.html("Hello, " + user.name + "! You are " + user.age + " years old!");
        });
    }

    @Override
    protected void configureRouting(WebServer server, Router router) {
        server.setRouter(router);
    }

    private static class User {
        public String name;
        public int age;
    }
}