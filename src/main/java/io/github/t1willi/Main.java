package io.github.t1willi;

import io.github.t1willi.core.JoltApplication;
import io.github.t1willi.form.Form;
import io.github.t1willi.security.session.Session;
import java.util.HashMap;
import java.util.Map;

public class Main extends JoltApplication {
    private static final Map<String, String> userDatabase = new HashMap<>();

    public static void main(String[] args) {
        userDatabase.put("admin", "adminpass");
        userDatabase.put("user1", "password1");

        launch(Main.class);
    }

    @Override
    public void init() {
        // Public routes
        get("/", ctx -> {
            String loggedIn = (String) Session.get("username");
            StringBuilder content = new StringBuilder();

            if (loggedIn != null) {
                content.append("Welcome back, ").append(loggedIn)
                        .append("! <a href='/dashboard'>Go to Dashboard</a> | <a href='/logout'>Logout</a>");
            } else {
                content.append("<h1>Login</h1>")
                        .append("<form action='/login' method='post'>")
                        .append("<input type='text' name='username' placeholder='Username'><br>")
                        .append("<input type='password' name='password' placeholder='Password'><br>")
                        .append("<button type='submit'>Login</button>")
                        .append("</form>");
            }

            return ctx.html(content.toString());
        });

        post("/login", ctx -> {
            Form form = ctx.buildForm();
            String username = form.getValue("username");
            String password = form.getValue("password");

            if (username != null && password != null &&
                    userDatabase.containsKey(username) &&
                    userDatabase.get(username).equals(password)) {

                Session.set("username", username);

                return ctx.redirect("/dashboard");
            }

            return ctx.html("Invalid credentials!").status(401);
        });

        get("/logout", ctx -> {
            Session.destroy();
            return ctx.redirect("/");
        });

        get("/dashboard", ctx -> {
            String username = (String) Session.get("username");
            if (username == null) {
                return ctx.redirect("/").status(302);
            }

            return ctx.html(
                    "<h1>Dashboard for " + username + "</h1>" +
                            "<p>You are authenticated!</p>" +
                            "<p>Session data:</p>" +
                            "<ul>" +
                            "<li>Username: " + username + "</li>" +
                            "<li>Access Time: " + Session.getAccess() + "</li>" +
                            "<li>Expire Time: " + Session.getExpire() + "</li>" +
                            "<li>IP: " + Session.getIpAddress() + "</li>" +
                            "<li>User Agent: " + Session.getUserAgent() + "</li>" +
                            "</ul>" +
                            "<a href='/logout'>Logout</a>");
        });

        get("/session-debug", ctx -> {
            return ctx.html(
                    "<h3>Session Debug Info</h3>" +
                            "<p>Session ID: " + Session.getSessionId() + "</p>");
        });
    }
}