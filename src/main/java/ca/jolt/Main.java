package ca.jolt;

import ca.jolt.core.JoltApplication;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    protected void setup() {
        post("/hello", ctx -> ctx.json("Hello, World!"));
        get("/", TodoController::index);

        group("/todos", () -> {
            post("/", TodoController::createTodo);
            post("/clear-completed", TodoController::clearCompletedTodos);
            post("/{id:int}", TodoController::updateTodo);
            get("/edit/{id:int}", TodoController::showEditForm);
            post("/toggle/{id:int}", TodoController::toggleTodoStatus);
            get("/delete/{id:int}", TodoController::deleteTodo);
            get("/", TodoController::getAllTodos);
        });

        group("/auth", () -> {
            get("/register", UserController::showRegisterForm);
            get("/login", UserController::showLoginForm);

            post("/register", UserController::register);
            post("/login", UserController::login);
            post("/logout", UserController::logout);
        });
    }
}