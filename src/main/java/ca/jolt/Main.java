package ca.jolt;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import ca.jolt.core.JoltApplication;

public class Main extends JoltApplication {

    private static final Map<String, String> sessions = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    protected void setup() {
        get("/", () -> "Hello, World!");
        get("/hello/{age:int}", ctx -> ctx.html("Hello " + ctx.query("name").orDefault("little one") + ", you are "
                + ctx.path("age").asInt() + " years old!"));
        post("/user", ctx -> {
            User user = ctx.body(User.class);
            return ctx.html("Hello, " + user.name() + "! You are " + user.age() + " years old!");
        });
        test();
    }

    private void test() {
        get("/login", ctx -> ctx.html(
                "<html>" +
                        "  <head><title>Login</title></head>" +
                        "  <body>" +
                        "    <h1>Login</h1>" +
                        "    <form method='post' action='/login'>" +
                        "      Username: <input type='text' name='username'/><br/>" +
                        "      Password: <input type='password' name='password'/><br/>" +
                        "      <input type='submit' value='Log In'/>" +
                        "    </form>" +
                        "  </body>" +
                        "</html>"));
        post("/login", ctx -> {
            String username = ctx.query("username").orDefault("");
            String password = ctx.query("password").orDefault("");

            if (!"admin".equals(username) || !"admin1".equals(password)) {
                return ctx.status(401).html(
                        "<html>" +
                                "  <head><title>Login Failed</title></head>" +
                                "  <body>" +
                                "    <h1>Invalid credentials</h1>" +
                                "    <p>Please try again.</p>" +
                                "    <a href='/login'>Back to Login</a>" +
                                "  </body>" +
                                "</html>");
            }

            // Generate a unique token and store it in the sessions map.
            String token = UUID.randomUUID().toString();
            sessions.put(token, username);

            // Set an HTTP‑only, secure cookie with the token.
            ctx.addCookie()
                    .setName("auth")
                    .setValue(token)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("Strict")
                    .path("/")
                    .build();

            return ctx.html(
                    "<html>" +
                            "  <head><title>Login Successful</title></head>" +
                            "  <body>" +
                            "    <h1>Login Successful</h1>" +
                            "    <p>Welcome, " + username + "!</p>" +
                            "    <a href='/verify'>Go to Verification Page</a>" +
                            "  </body>" +
                            "</html>");
        });

        get("/verify", ctx -> {
            var cookie = ctx.getCookie("auth");
            if (cookie == null || !sessions.containsKey(cookie.getValue())) {
                return ctx.html(
                        "<html>" +
                                "  <head><title>Not Authenticated</title></head>" +
                                "  <body>" +
                                "    <h1>You are not authenticated</h1>" +
                                "    <a href='/login'>Log In</a>" +
                                "  </body>" +
                                "</html>");
            }
            String username = sessions.get(cookie.getValue());
            return ctx.html(
                    "<html>" +
                            "  <head><title>Verify</title></head>" +
                            "  <body>" +
                            "    <h1>Authenticated as " + username + "</h1>" +
                            "    <form method='get' action='/logout'>" +
                            "      <button type='submit'>Log Out</button>" +
                            "    </form>" +
                            "  </body>" +
                            "</html>");
        });

        get("/logout", ctx -> {
            var cookie = ctx.getCookie("auth");
            if (cookie != null) {
                sessions.remove(cookie.getValue());
                ctx.removeCookie("auth");
            }
            return ctx.html(
                    "<html>" +
                            "  <head><title>Logged Out</title></head>" +
                            "  <body>" +
                            "    <h1>You have been logged out</h1>" +
                            "    <a href='/login'>Log In Again</a>" +
                            "  </body>" +
                            "</html>");
        });
    }

    private static record User(String name, int age) {
    }
}