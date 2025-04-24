package io.github.t1willi;

import io.github.t1willi.core.JoltApplication;
import io.github.t1willi.form.Form;
import io.github.t1willi.http.HttpStatus;
import io.github.t1willi.security.session.Session;
import io.github.t1willi.template.JoltModel;

public class Main extends JoltApplication {

    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    protected void init() {
        get("/", ctx -> ctx.html("<a href=\"/login\">Login</a>"));

        get("/login", ctx -> ctx.render("login", null));

        post("/login", ctx -> {
            Form form = ctx.buildForm();
            String username = form.getValue("username");
            String password = form.getValue("password");
            if ("admin".equals(username) && "password".equals(password)) {
                Session.setAuthenticated(true);
                Session.set("user", username);
                return ctx.redirect("/dashboard");
            } else {
                return ctx.status(HttpStatus.BAD_REQUEST).render("login", JoltModel.of("error", "Invalid credentials"));
            }
        });

        get("/logout", ctx -> {
            Session.destroy();
            return ctx.redirect("/login");
        });

        get("/debug-session", ctx -> {
            boolean isAuth = Session.isAuthenticated();
            String user = Session.get("user");
            return ctx.text("Is authenticated: " + isAuth + ", User: " + user);
        });
    }
}