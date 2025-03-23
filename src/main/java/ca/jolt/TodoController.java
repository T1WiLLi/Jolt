package ca.jolt;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

        // Get all todos for this user
        List<Todo> userTodos = getTodosByUsername(username);

        // Get filter parameters
        String filterStatus = ctx.query("status").orDefault("all");
        String sortBy = ctx.query("sort").orDefault("date");
        String searchQuery = ctx.query("search").orDefault("");
        String priority = ctx.query("priority").orDefault("");
        String category = ctx.query("category").orDefault("");

        // Apply filters
        List<Todo> filteredTodos = filterAndSortTodos(userTodos, filterStatus, sortBy, searchQuery, priority, category);

        // Calculate statistics
        int totalCount = userTodos.size();
        int completedCount = (int) userTodos.stream().filter(Todo::isCompleted).count();
        int pendingCount = totalCount - completedCount;

        // Get today's date for highlighting today's tasks
        String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // Create message based on completion ratio
        String message = createMotivationalMessage(completedCount, totalCount);

        JoltModel model = JoltModel.create()
                .with("title", "Todo Manager")
                .with("message", message)
                .with("todos", filteredTodos)
                .with("totalCount", totalCount)
                .with("completedCount", completedCount)
                .with("pendingCount", pendingCount)
                .with("filterStatus", filterStatus)
                .with("sortBy", sortBy)
                .with("searchQuery", searchQuery)
                .with("priority", priority)
                .with("category", category)
                .with("today", today);

        return ctx.render("home.ftl", model);
    }

    private static String createMotivationalMessage(int completed, int total) {
        if (total == 0) {
            return "Ready to start organizing your tasks!";
        }

        double completionRatio = (double) completed / total;

        if (completionRatio == 1.0) {
            return "Congratulations! All tasks completed. You're amazing!";
        } else if (completionRatio >= 0.8) {
            return "Almost there! Keep up the great work!";
        } else if (completionRatio >= 0.5) {
            return "Halfway there! You're making good progress.";
        } else if (completionRatio >= 0.25) {
            return "Good start! Keep going!";
        } else if (completed > 0) {
            return "You've made a start. You can do this!";
        } else {
            return "Let's tackle your tasks one by one!";
        }
    }

    private static List<Todo> filterAndSortTodos(List<Todo> todos, String filterStatus, String sortBy,
            String searchQuery, String priority, String category) {
        // First filter by status
        List<Todo> filtered = todos.stream()
                .filter(todo -> {
                    if ("completed".equals(filterStatus)) {
                        return todo.isCompleted();
                    } else if ("pending".equals(filterStatus)) {
                        return !todo.isCompleted();
                    } else if ("today".equals(filterStatus)) {
                        return todo.getDate().equals(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
                    }
                    return true; // "all" filter
                })
                .collect(Collectors.toList());

        // Then filter by search query if provided
        if (!searchQuery.isEmpty()) {
            filtered = filtered.stream()
                    .filter(todo -> todo.getText().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            todo.getDescription().toLowerCase().contains(searchQuery.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Filter by priority if provided
        if (!priority.isEmpty()) {
            filtered = filtered.stream()
                    .filter(todo -> todo.getPriority().equals(priority))
                    .collect(Collectors.toList());
        }

        // Filter by category if provided
        if (!category.isEmpty()) {
            filtered = filtered.stream()
                    .filter(todo -> todo.getCategory().equals(category))
                    .collect(Collectors.toList());
        }

        // Finally sort
        Comparator<Todo> comparator = null;
        switch (sortBy) {
            case "alpha":
                comparator = Comparator.comparing(Todo::getText);
                break;
            case "date-asc":
                comparator = Comparator.comparing(Todo::getDate);
                break;
            case "date-desc":
                comparator = Comparator.comparing(Todo::getDate).reversed();
                break;
            case "completed":
                comparator = Comparator.comparing(Todo::isCompleted).reversed();
                break;
            default: // date by default
                comparator = Comparator.comparing(Todo::getDate);
        }

        return filtered.stream().sorted(comparator).collect(Collectors.toList());
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
            String priority = (String) form.getValue("priority");
            String category = (String) form.getValue("category");

            Todo newTodo = new Todo(idGenerator.incrementAndGet(), text, completed, description, date, username,
                    priority, category);
            createTodo(newTodo);

            return ctx.redirect("/");
        } catch (Exception e) {
            return ctx.redirect("/?error=" + e.getMessage());
        }
    }

    public static void createTodo(Todo todo) {
        todoStore.put(todo.getId(), todo);
    }

    public static JoltContext updateTodo(JoltContext ctx) {
        try {
            Cookie cookie = ctx.getCookie("session");
            if (cookie == null) {
                return ctx.status(401).json(Map.of("error", "Unauthorized"));
            }
            String username = JwtToken.getOwner(cookie.getValue());
            if (username == null) {
                return ctx.status(401).json(Map.of("error", "Unauthorized"));
            }

            int id = ctx.path("id").asInt();
            Form form = ctx.buildForm();

            Todo existingTodo = todoStore.get(id);
            if (existingTodo == null || !existingTodo.getUsername().equals(username)) {
                return ctx.redirect("/?error=Todo+not+found");
            }

            String text = (String) form.getValue("text");
            boolean completed = form.getValue("completed") != null;
            String description = (String) form.getValue("description");
            String date = (String) form.getValue("date");
            String priority = (String) form.getValue("priority");
            String category = (String) form.getValue("category");

            existingTodo.setText(text);
            existingTodo.setCompleted(completed);
            existingTodo.setDescription(description);
            existingTodo.setDate(date);
            existingTodo.setPriority(priority);
            existingTodo.setCategory(category);

            todoStore.put(id, existingTodo);

            return ctx.redirect(
                    "/" + (form.getValue("returnFilter") != null ? "?status=" + form.getValue("returnFilter") : ""));
        } catch (Exception e) {
            return ctx.redirect("/?error=" + e.getMessage());
        }
    }

    public static JoltContext toggleTodoStatus(JoltContext ctx) {
        try {
            String username = JwtToken.getOwner(ctx.getCookie("session").getValue());
            if (username == null) {
                return ctx.redirect("/auth/login");
            }

            int id = ctx.path("id").asInt();

            Todo existingTodo = todoStore.get(id);
            if (existingTodo == null || !existingTodo.getUsername().equals(username)) {
                return ctx.redirect("/?error=Todo+not+found");
            }

            // Toggle the completed status
            existingTodo.setCompleted(!existingTodo.isCompleted());
            todoStore.put(id, existingTodo);

            // Get the current filter to maintain it after redirect
            String currentFilter = ctx.query("returnFilter").orDefault("");
            return ctx.redirect("/" + (!currentFilter.isEmpty() ? "?status=" + currentFilter : ""));
        } catch (Exception e) {
            return ctx.redirect("/?error=" + e.getMessage());
        }
    }

    public static JoltContext deleteTodo(JoltContext ctx) {
        try {
            String username = JwtToken.getOwner(ctx.getCookie("session").getValue());
            if (username == null) {
                return ctx.redirect("/auth/login");
            }

            int id = ctx.path("id").asInt();

            Todo removedTodo = todoStore.get(id);
            if (removedTodo == null || !removedTodo.getUsername().equals(username)) {
                return ctx.redirect("/?error=Todo+not+found");
            }

            todoStore.remove(id);

            // Get the current filter to maintain it after redirect
            String currentFilter = ctx.query("returnFilter").orDefault("");
            return ctx.redirect("/" + (!currentFilter.isEmpty() ? "?status=" + currentFilter : ""));
        } catch (Exception e) {
            return ctx.redirect("/?error=" + e.getMessage());
        }
    }

    public static JoltContext showEditForm(JoltContext ctx) {
        try {
            String username = JwtToken.getOwner(ctx.getCookie("session").getValue());
            if (username == null) {
                return ctx.redirect("/auth/login");
            }

            int id = ctx.path("id").asInt();
            Todo todo = todoStore.get(id);

            if (todo == null || !todo.getUsername().equals(username)) {
                return ctx.redirect("/?error=Todo+not+found");
            }

            String returnFilter = ctx.query("returnFilter").orDefault("all");

            JoltModel model = JoltModel.create()
                    .with("title", "Edit Todo")
                    .with("todo", todo)
                    .with("returnFilter", returnFilter);

            return ctx.render("edit-todo.ftl", model);
        } catch (Exception e) {
            return ctx.redirect("/?error=" + e.getMessage());
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

    public static JoltContext clearCompletedTodos(JoltContext ctx) {
        try {
            String username = JwtToken.getOwner(ctx.getCookie("session").getValue());
            if (username == null) {
                return ctx.redirect("/auth/login");
            }

            List<Integer> toRemove = new ArrayList<>();
            for (Todo todo : todoStore.values()) {
                if (todo.getUsername().equals(username) && todo.isCompleted()) {
                    toRemove.add(todo.getId());
                }
            }

            for (Integer id : toRemove) {
                todoStore.remove(id);
            }

            return ctx.redirect("/");
        } catch (Exception e) {
            return ctx.redirect("/?error=" + e.getMessage());
        }
    }
}