package ca.jolt;

import ca.jolt.core.JoltApplication;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class, "ca.jolt");
    }

    @Override
    public void setup() {
        get("/", ctx -> ctx.html("Hello World!"));
    }
}
