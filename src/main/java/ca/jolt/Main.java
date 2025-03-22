package ca.jolt;

import ca.jolt.core.JoltApplication;
import ca.jolt.template.JoltModel;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    protected void setup() {
        get("/", ctx -> {
            JoltModel model = JoltModel.create()
                    .with("title", "Welcome to Jolt X Freemarker")
                    .with("message", "Hello, world!")
                    .with("items", new String[] { "item1", "item2", "item3" });

            return ctx.render("home.ftl", model);
        });
    }
}
