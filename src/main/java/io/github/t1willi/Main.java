package io.github.t1willi;

import io.github.t1willi.core.JoltApplication;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class);
    }

    public void init() {
        get("/", ctx -> ctx.html("Hello World!"));
        post("/lol", ctx -> ctx.json(ctx));
    }
}
