package ca.jolt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ca.jolt.form.Form;
import ca.jolt.routing.context.JoltContext;
import ca.jolt.template.JoltModel;

public class TodoController {

    private static final Map<Integer, Todo> todoStore = new HashMap<>();
    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    public static void createInitialTodos() {
        createTodo(new Todo(idGenerator.incrementAndGet(), "Learn Jolt Framework", true));
        createTodo(new Todo(idGenerator.incrementAndGet(), "Build awesome web apps", false));
        createTodo(new Todo(idGenerator.incrementAndGet(), "Integrate Freemarker", true));
        createTodo(new Todo(idGenerator.incrementAndGet(), "Deploy to production", false));
    }

    public static JoltContext index(JoltContext ctx) {
        JoltModel model = JoltModel.create()
                .with("title", "Jolt X Freemarker Todo Demo")
                .with("message", "Welcome to this interactive demo!")
                .with("todos", todoStore.values());

        return ctx.render("home.ftl", model);
    }

    public static JoltContext createTodo(JoltContext ctx) {
        try {
            Form form = ctx.buildForm();
            String text = (String) form.getValue("text");
            boolean completed = form.getValue("completed") != null
                    && Boolean.parseBoolean((String) form.getValue("completed"));

            Todo newTodo = new Todo(idGenerator.incrementAndGet(), text, completed);
            createTodo(newTodo);

            return ctx.json(newTodo);
        } catch (Exception e) {
            return ctx.status(400).json(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }

    public static void createTodo(Todo todo) {
        todoStore.put(todo.getId(), todo);
    }

    public static JoltContext updateTodo(JoltContext ctx) {
        try {
            int id = ctx.path("id").asInt();
            Form form = ctx.buildForm();

            Todo existingTodo = todoStore.get(id);
            if (existingTodo == null) {
                return ctx.status(404).json(Map.of("error", "Todo not found"));
            }
            existingTodo = form.updateEntity(existingTodo, "id");

            todoStore.put(id, existingTodo);
            return ctx.json(existingTodo);
        } catch (Exception e) {
            return ctx.status(400).json(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }

    public static JoltContext deleteTodo(JoltContext ctx) {
        try {
            int id = ctx.path("id").asInt();

            Todo removedTodo = todoStore.remove(id);
            if (removedTodo == null) {
                return ctx.status(404).json(Map.of("error", "Todo not found"));
            }

            return ctx.json(Map.of("success", true, "id", id));
        } catch (Exception e) {
            return ctx.status(400).json(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }

    public static JoltContext getAllTodos(JoltContext ctx) {
        List<Todo> todos = new ArrayList<>(todoStore.values());
        return ctx.json(todos);
    }
}
