package ca.jolt;

import ca.jolt.core.JoltApplication;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    protected void setup() {
        get("/", () -> "Hello, World!");
        get("/hello/{age:int}", ctx -> ctx.html("Hello " + ctx.query("name").orDefault("little one") + ", you are "
                + ctx.path("age").asInt() + " years old!"));
        post("/user", ctx -> {
            User user = ctx.body(User.class);
            return ctx.html("Hello, " + user.name() + "! You are " + user.age() + " years old!");
        });
    }

    private static record User(String name, int age) {
    }
}