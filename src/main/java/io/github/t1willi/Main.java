package io.github.t1willi;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.github.t1willi.context.JoltContext;
import io.github.t1willi.core.JoltApplication;
import io.github.t1willi.form.Form;
import io.github.t1willi.http.Http;
import io.github.t1willi.http.Response;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    public void init() {
        get("", ctx -> ctx.html("Hello World!"));
        get("/pokemon", this::getPokemons);
        post("/form", ctx -> {
            Form form = ctx.buildForm();
            String name = form.getValue("name");
            String email = form.getValue("email");
            String message = form.getValue("message");
            return ctx.json(Map.of("name", name, "email", email, "message", message));
        });
    }

    private JoltContext getPokemons(JoltContext ctx) {
        try {
            Response response = Http.get("https://pokeapi.co/api/v2/pokemon?limit=1000").execute();
            Pokedex pokedex = response.json(Pokedex.class);
            return ctx.json(pokedex);
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