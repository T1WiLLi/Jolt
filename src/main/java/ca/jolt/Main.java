package ca.jolt;

import ca.jolt.core.JoltApplication;
import ca.jolt.form.Form;
import ca.jolt.form.Rule;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Main extends JoltApplication {

    // Session storage for login.
    private static final Map<String, String> sessions = new ConcurrentHashMap<>();
    // In-memory storage for registered users, keyed by email (stored in lowercase).
    private static final Map<String, RegisteredUser> registeredUsers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    protected void setup() {
        get("/", ctx -> "Hello, World!");

        // Example route with typed path parameters.
        get("/hello/{age:int}", ctx -> ctx.html(
                "Hello " + ctx.query("name").orDefault("little one") + ", you are "
                        + ctx.path("age").asInt() + " years old!"));

        // Simple POST endpoint to echo a User JSON object.
        post("/user", ctx -> {
            User user = ctx.body(User.class);
            if (user == null) {
                return ctx.status(400).html("Invalid user data");
            }
            return ctx.html("Hello, " + user.name() + "! You are " + user.age() + " years old!");
        });

        // Define login and registration routes.
        defineLoginRoutes();
        defineRegistrationRoutes();
    }

    private void defineLoginRoutes() {
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
            // Build a Form from query parameters (or form-encoded body)
            Form form = ctx.queryToForm();

            form.field("username")
                    .trim().toLowerCase()
                    .required("Username is required.")
                    .minLength(3, "Username must be at least 3 characters long.")
                    .maxLength(20, "Username must be at most 20 characters long.")
                    .alphanumeric("Username must contain only letters and numbers.");

            form.field("password")
                    .trim()
                    .required("Password is required.")
                    .minLength(6, "Password must be at least 6 characters long.");

            if (!form.verify()) {
                StringBuilder sb = new StringBuilder();
                sb.append("<html><head><title>Validation Error</title></head><body>");
                sb.append("<h1>Validation Errors:</h1>");
                form.getErrors().forEach((field, error) -> sb.append("<p><strong>").append(field).append("</strong>: ")
                        .append(error).append("</p>"));
                sb.append("<a href='/login'>Back to Login</a>");
                sb.append("</body></html>");
                return ctx.status(400).html(sb.toString());
            }

            String username = form.getValue("username");
            String password = form.getValue("password");

            // Simple credential check (login only allows admin/admin1).
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

            String token = UUID.randomUUID().toString();
            sessions.put(token, username);

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

    private void defineRegistrationRoutes() {
        // Display registration form.
        get("/register", ctx -> ctx.html(
                "<html>" +
                        "  <head><title>Register</title></head>" +
                        "  <body>" +
                        "    <h1>Register</h1>" +
                        "    <form method='post' action='/register'>" +
                        "      Name: <input type='text' name='name'/><br/>" +
                        "      Age: <input type='text' name='age'/><br/>" +
                        "      Date of Birth (yyyy-MM-dd): <input type='text' name='dob'/><br/>" +
                        "      Email: <input type='text' name='email'/><br/>" +
                        "      <input type='submit' value='Register'/>" +
                        "    </form>" +
                        "  </body>" +
                        "</html>"));

        post("/register", ctx -> {
            // Build a form from request body (JSON or form-encoded).
            Form form = ctx.bodyToForm();

            // Fluent rule chaining for registration fields.
            form.field("name")
                    .trim()
                    .required("Name is required.")
                    // Prevent duplicate names (case-insensitive).
                    .addRules(
                            Rule.custom(
                                    (data, allValues) -> registeredUsers.values().stream()
                                            .noneMatch(u -> u.name().equalsIgnoreCase(data)),
                                    "This name is already taken."));

            form.field("age")
                    .trim()
                    .required("Age is required.")
                    .asInt()
                    .min(18, "You must be at least 18 years old.");

            // Combine type conversion and validation into one call.
            form.field("dob")
                    .trim()
                    .required("Date of birth is required.")
                    .date("yyyy-MM-dd"); // This method both registers the date pattern and validates.

            form.field("email")
                    .trim().toLowerCase()
                    .required("Email is required.")
                    .email("Invalid email format.")
                    // Prevent duplicate emails.
                    .addRules(
                            Rule.custom(
                                    (data, allValues) -> !registeredUsers.containsKey(data),
                                    "This email is already registered."));

            if (!form.verify()) {
                StringBuilder sb = new StringBuilder();
                sb.append("<html><head><title>Registration Error</title></head><body>");
                sb.append("<h1>Registration Errors:</h1>");
                form.getErrors().forEach((field, error) -> sb.append("<p><strong>").append(field).append("</strong>: ")
                        .append(error).append("</p>"));
                sb.append("<a href='/register'>Back to Registration</a>");
                sb.append("</body></html>");
                return ctx.status(400).html(sb.toString());
            }

            // If validation passes, create a new RegisteredUser.
            String name = form.getValue("name");
            Integer age = form.getValueAsInt("age");
            LocalDate dob = form.getValueAsDate("dob", "yyyy-MM-dd");
            String email = form.getValue("email");

            RegisteredUser user = new RegisteredUser(name, age, dob, email);
            registeredUsers.put(email, user);

            return ctx.html(
                    "<html>" +
                            "  <head><title>Registration Successful</title></head>" +
                            "  <body>" +
                            "    <h1>Registration Successful</h1>" +
                            "    <p>Welcome, " + name + "!</p>" +
                            "    <p>Your registered email: " + email + "</p>" +
                            "    <a href='/login'>Go to Login</a>" +
                            "  </body>" +
                            "</html>");
        });

        get("/registered", ctx -> {
            StringBuilder sb = new StringBuilder();
            sb.append("<html><head><title>Registered Users</title></head><body>");
            sb.append("<h1>Registered Users</h1>");
            if (registeredUsers.isEmpty()) {
                sb.append("<p>No users registered.</p>");
            } else {
                registeredUsers.values().forEach(user -> {
                    sb.append("<p>").append(user).append("</p>");
                });
            }
            sb.append("<a href='/register'>Register another user</a>");
            sb.append("</body></html>");
            return ctx.html(sb.toString());
        });
    }

    // Example record for JSON deserialization in /user endpoint.
    private static record User(String name, int age) {
    }

    // A simple registered user record.
    private static record RegisteredUser(String name, Integer age, LocalDate dob, String email) {
        @Override
        public String toString() {
            return name + " (" + email + "), Age: " + age + ", DOB: " + dob;
        }
    }
}
