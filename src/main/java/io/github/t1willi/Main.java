package io.github.t1willi;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.core.JoltApplication;
import io.github.t1willi.form.Form;
import io.github.t1willi.http.api.HttpClient;
import io.github.t1willi.http.api.HttpClientFactory;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    public void init() {
        get("", ctx -> ctx.html("Hello World!"));
        get("/pokemon", this::getPokemons);
        get("/exception", ctx -> {
            throw new RuntimeException("WTF BITCH");
        });
        post("/form", ctx -> {
            Form form = ctx.buildForm();

            form.field("message")
                    .required("The message is required.");
            form.field("name")
                    .required("The name is required.");
            form.field("email")
                    .email();

            if (!form.validate()) {
                return ctx.json(form.errors());
            }

            String name = form.field("name").get();
            String email = form.field("email").get();
            String message = form.field("message").get();
            return ctx.json(Map.of("name", name, "email", email, "message", message));
        });

        group("/api", 1, () -> {
            group("/product", () -> {
                get("", ctx -> ctx.text("Des produits"));
            });
            get("/user", ctx -> ctx
                    .json(Map.of("User", Map.of("name", "Wlliam Beaudin", "age", 20, "profession", "Developer"))));
        });
    }

    private JoltContext getPokemons(JoltContext ctx) {
        try (HttpClient client = HttpClientFactory.create(Duration.ofSeconds(5), false)) {
            return ctx.json(client.async("https://pokeapi.co/api/v2/pokemon?limit=1000").as(Pokedex.class));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return ctx.abortInternalServerError("Error fetching pokemons");
    }

    public static class Pokedex {
        public long count;
        public String next;
        public Object previous;
        public List<Result> results;
    }

    public static class Result {
        public String name;
        public String url;
    }
}