package io.github.t1willi;

import io.github.t1willi.core.JoltApplication;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    public void init() {
        get("/", ctx -> ctx.plain("Hello, World!"));
        get("/user/{id}", ctx -> ctx.html("Hello, User #" + ctx.path("id")));
    }
}
