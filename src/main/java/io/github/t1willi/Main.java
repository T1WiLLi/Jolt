package io.github.t1willi;

import io.github.t1willi.core.JoltApplication;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch();
    }

    @Override
    public void init() {
        get("/hello", ctx -> ctx.html("Hello, World!"));
    }
}
