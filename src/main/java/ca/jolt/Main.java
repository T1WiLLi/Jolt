package ca.jolt;

import java.util.Map;

import ca.jolt.core.JoltApplication;
import ca.jolt.http.HttpStatus;
import ca.jolt.routing.context.JoltContext;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class, "ca.jolt");
    }

    @Override
    public void setup() {
        get("/", Main::getUser);
    }

    public static JoltContext getUser(JoltContext ctx) {
        return ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("name", "John Doe", "age", 30));
    }
}
