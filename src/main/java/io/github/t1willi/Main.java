package io.github.t1willi;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.t1willi.core.JoltApplication;
import io.github.t1willi.template.JoltModel;

public class Main extends JoltApplication {

    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    protected void init() {
        get("/freemarker", ctx -> {
            return ctx.render("freemarker", createTestModel());
        });
        get("/thymeleaf", ctx -> {
            return ctx.render("thymeleaf", createTestModel());
        });

        get("/json", ctx -> {
            List<User> users = createList();
            return ctx.json(users);
        });
    }

    private static List<User> createList() {
        return Arrays.asList(
                new User("Alice", "alice@example.com", "password123"),
                new User("Jordan", "jordan@gmail.com", "password123"),
                new User("Bob", "bob@example.com", "password123"),
                new User("Charlie", "charlie@example.com", "password123"));
    }

    private static JoltModel createTestModel() {
        Map<String, Object> user = new HashMap<>();
        user.put("name", "John Doe");
        user.put("email", "john.doe@example.com");

        return JoltModel.create()
                .with("title", "Template Engine Test")
                .with("message", "This is a test message from JoltModel")
                .with("user", user)
                .with("items", Arrays.asList("Item One", "Item Two", "Item Three"))
                .with("showSection", true)
                .with("date", new Date())
                .with("number", 12345.6789)
                .with("copyright", "© 2025 Your Company");
    }
}
