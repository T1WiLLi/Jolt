package ca.jolt;

import ca.jolt.core.JoltApplication;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    protected void setup() {
        TodoController.createInitialTodos();
        get("/lmao", ctx -> {
            return ctx.write("lmao").contentType("text/html");
        });
        get("/", TodoController::index);
        group("/todos", () -> {
            post("/", TodoController::createTodo);
            put("/{id:int}", TodoController::updateTodo);
            delete("/{id:int}", TodoController::deleteTodo);
            get("/", TodoController::getAllTodos);
        });
    }
}
