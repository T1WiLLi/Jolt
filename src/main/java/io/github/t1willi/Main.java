package io.github.t1willi;

import io.github.t1willi.core.JoltApplication;
import io.github.t1willi.template.JoltModel;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    public void init() {
        get("/", ctx -> ctx.html("Hello World!"));
        get("/xss-test", ctx -> {
            String input = ctx.query("input");
            JoltModel model = JoltModel.create().with("input", input != null ? input : "");
            return ctx.render("xss-test", model);
        });
    }
}
