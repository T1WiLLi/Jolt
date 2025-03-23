package ca.jolt;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ca.jolt.security.utils.JwtToken;
import ca.jolt.routing.context.JoltContext;
import ca.jolt.template.JoltModel;

public class UserController {

    private static final Map<String, User> userStore = new HashMap<>();
    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    public static JoltContext showRegisterForm(JoltContext ctx) {
        JoltModel model = JoltModel.create()
                .with("title", "Register")
                .with("message", "Create a new account")
                .with("error", ctx.query("error").orDefault(""));
        return ctx.render("register.ftl", model);
    }

    public static JoltContext register(JoltContext ctx) {
        try {
            String username = ctx.query("username").get();
            String password = ctx.query("password").get();

            if (userStore.containsKey(username)) {
                return ctx.redirect("/auth/register?error=Username+already+exists");
            }

            User newUser = new User(idGenerator.incrementAndGet(), username, password);
            userStore.put(username, newUser);

            return ctx.redirect("/auth/login");
        } catch (Exception e) {
            return ctx.redirect("/auth/register?error=Invalid+request");
        }
    }

    public static JoltContext showLoginForm(JoltContext ctx) {
        JoltModel model = JoltModel.create()
                .with("title", "Login")
                .with("message", "Welcome back! Please log in")
                .with("error", ctx.query("error").orDefault(""));
        return ctx.render("login.ftl", model);
    }

    public static JoltContext login(JoltContext ctx) {
        try {
            String username = ctx.query("username").get();
            String password = ctx.query("password").get();

            User user = userStore.get(username);
            if (user == null || !user.getPassword().equals(password)) {
                return ctx.redirect("/auth/login?error=Invalid+username+or+password");
            }

            String token = JwtToken.create(user.getUsername());
            ctx.addCookie()
                    .setName("session")
                    .setValue(token)
                    .httpOnly(true)
                    .secure(true)
                    .maxAge(1800)
                    .path("/")
                    .sameSite("Strict")
                    .build();

            return ctx.redirect("/");
        } catch (Exception e) {
            return ctx.redirect("/auth/login?error=Invalid+request");
        }
    }

    public static JoltContext logout(JoltContext ctx) {
        return ctx.redirect("/auth/login").removeCookie("session");
    }
}