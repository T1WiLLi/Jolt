package io.github.t1willi;

import io.github.t1willi.core.JoltApplication;
import io.github.t1willi.utils.Flash;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    public void init() {
        get("/*", ctx -> {
            Flash.error("La page que vous avez demandée n'existe pas: " + ctx.rawPath());
            return ctx.redirect("/");
        });
        get("/", ctx -> ctx.render("index", null));

        post("/flash-test/success", ctx -> {
            Flash.success("Operation completed successfully!");
            return ctx.redirect("/");
        });
        post("/flash-test/error", ctx -> {
            Flash.error("Operation failed!");
            return ctx.redirect("/");
        });
        post("/flash-test/info", ctx -> {
            Flash.info("Operation completed with info!");
            return ctx.redirect("/");
        });
        post("/flash-test/warning", ctx -> {
            Flash.warning("Operation completed with warning!");
            return ctx.redirect("/");
        });
        post("/flash-test/clear", ctx -> {
            Flash.clear();
            return ctx.redirect("/");
        });
    }
}
