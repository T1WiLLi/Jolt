package ca.jolt;

import java.util.Map;

import ca.jolt.core.JoltApplication;
import ca.jolt.routing.context.JoltContext;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class, "ca.jolt");
    }

    @Override
    public void setup() {
        get("/", Main::getUser);
        group("/product", () -> {
            get("", ProductController::getAll);
            get("/{id:int}", ProductController::get);
            post("", ProductController::create);
            put("/{id:int}", ProductController::update);
            delete("/{id:int}", ProductController::delete);
        });
    }

    public static JoltContext getUser(JoltContext ctx) {
        return ctx.json(Map.of("name", "John Doe", "age", 30));
    }
}
