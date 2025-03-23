package ca.jolt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ca.jolt.form.Form;
import ca.jolt.routing.context.JoltContext;
import ca.jolt.security.utils.JwtToken;
import ca.jolt.template.JoltModel;
import jakarta.servlet.http.Cookie;

public class TodoController {

    private static final Map<Integer, Todo> todoStore = new HashMap<>();
    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    public static JoltContext index(JoltContext ctx) {
        Cookie cookie = ctx.getCookie("session");
        if (cookie == null) {
            return ctx.redirect("/auth/login");
        }
        String username = JwtToken.getOwner(cookie.getValue());
        if (username == null) {
            return ctx.redirect("/auth/login");
        }

        JoltModel model = JoltModel.create()
                .with("title", "Jolt X Freemarker Todo Demo")
                .with("message", "Welcome to this interactive demo!")
                .with("todos", getTodosByUsername(username));

        return ctx.render("home.ftl", model);
    }

    public static JoltContext createTodo(JoltContext ctx) {
        try {
            Cookie cookie = ctx.getCookie("session");
            if (cookie == null) {
                return ctx.status(401).json(Map.of("error", "Unauthorized"));
            }
            String username = JwtToken.getOwner(cookie.getValue());
            if (username == null) {
                return ctx.status(401).json(Map.of("error", "Unauthorized"));
            }

            Form form = ctx.buildForm();
            String text = (String) form.getValue("text");
            boolean completed = form.getValue("completed") != null
                    && Boolean.parseBoolean((String) form.getValue("completed"));
            String description = (String) form.getValue("description");
            String date = (String) form.getValue("date");

            Todo newTodo = new Todo(idGenerator.incrementAndGet(), text, completed, description, date, username);
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
            String username = JwtToken.getOwner(ctx.getCookie("session").getValue());
            if (username == null) {
                return ctx.status(401).json(Map.of("error", "Unauthorized"));
            }

            int id = ctx.path("id").asInt();
            Form form = ctx.buildForm();

            Todo existingTodo = todoStore.get(id);
            if (existingTodo == null || !existingTodo.getUsername().equals(username)) {
                return ctx.status(404).json(Map.of("error", "Todo not found"));
            }

            existingTodo.setText((String) form.getValue("text"));
            existingTodo.setCompleted(Boolean.parseBoolean((String) form.getValue("completed")));
            existingTodo.setDescription((String) form.getValue("description"));
            existingTodo.setDate((String) form.getValue("date"));

            todoStore.put(id, existingTodo);
            return ctx.json(existingTodo);
        } catch (Exception e) {
            return ctx.status(400).json(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }

    public static JoltContext deleteTodo(JoltContext ctx) {
        try {
            String username = JwtToken.getOwner(ctx.getCookie("session").getValue());
            if (username == null) {
                return ctx.status(401).json(Map.of("error", "Unauthorized"));
            }

            int id = ctx.path("id").asInt();

            Todo removedTodo = todoStore.remove(id);
            if (removedTodo == null || !removedTodo.getUsername().equals(username)) {
                return ctx.status(404).json(Map.of("error", "Todo not found"));
            }

            return ctx.json(Map.of("success", true, "id", id));
        } catch (Exception e) {
            return ctx.status(400).json(Map.of("error", "Invalid request: " + e.getMessage()));
        }
    }

    public static JoltContext getAllTodos(JoltContext ctx) {
        String username = JwtToken.getOwner(ctx.getCookie("session").getValue());
        if (username == null) {
            return ctx.status(401).json(Map.of("error", "Unauthorized"));
        }

        List<Todo> todos = getTodosByUsername(username);
        return ctx.json(todos);
    }

    private static List<Todo> getTodosByUsername(String username) {
        List<Todo> userTodos = new ArrayList<>();
        for (Todo todo : todoStore.values()) {
            if (todo.getUsername().equals(username)) {
                userTodos.add(todo);
            }
        }
        return userTodos;
    }
}