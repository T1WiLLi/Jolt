package ca.jolt;

import ca.jolt.core.JoltApplication;
import ca.jolt.files.JoltFile;
import ca.jolt.form.Form;
import ca.jolt.form.Rule;
import ca.jolt.http.Http;
import ca.jolt.http.HttpStatus;
import ca.jolt.http.Response;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Main extends JoltApplication {

        // Session storage for login.
        private static final Map<String, String> sessions = new ConcurrentHashMap<>();
        // In-memory storage for registered users, keyed by email (stored in lowercase).
        private static final Map<String, RegisteredUser> registeredUsers = new ConcurrentHashMap<>();

        private static final Map<String, JoltFile> uploadedFiles = new ConcurrentHashMap<>();

        private static final String WEATHER_API_URL = "wttr.in";

        public static void main(String[] args) {
                launch(Main.class, "ca.jolt");
        }

        @Override
        protected void setup() {
                group("/api", () -> {
                        get("/test", () -> "Hello Test");
                });

                get("/", () -> "Hello World");

                get("/redirect", ctx -> {
                        if (ctx.query("redirect").asBooleanOrDefault(false)) {
                                return ctx.redirect("/redirect2", () -> {
                                        get("/redirect2", newCtx -> newCtx.html("You have been redirected"));
                                });
                        } else {
                                return ctx.html("You are not being redirected");
                        }
                });

                get("/doc", ctx -> ctx.serve("index.html"));

                // Example route with typed path parameters.
                get("/hello/{age:int}", ctx -> ctx.html(
                                "Hello " + ctx.query("name").orDefault("little one") + ", you are "
                                                + ctx.path("age").asInt() + " years old!")
                                .status(HttpStatus.OK));

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
                defineUploadRoutes();
                defineWeatherRoutes();
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
                        Form form = ctx.buildForm();

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
                                form.getErrors().forEach((field, error) -> sb.append("<p><strong>").append(field)
                                                .append("</strong>: ")
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

                        ctx.addCookie().sessionCookie(token);

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
                                                "      Date of Birth (yyyy-MM-dd): <input type='text' name='dob'/><br/>"
                                                +
                                                "      Email: <input type='text' name='email'/><br/>" +
                                                "      <input type='submit' value='Register'/>" +
                                                "    </form>" +
                                                "  </body>" +
                                                "</html>"));

                post("/register", ctx -> {
                        // Build a form from request body (JSON or form-encoded).
                        Form form = ctx.buildForm();

                        // Fluent rule chaining for registration fields.
                        form.field("name")
                                        .trim()
                                        .required("Name is required.")
                                        // Prevent duplicate names (case-insensitive).
                                        .addRules(
                                                        Rule.custom(
                                                                        (data) -> registeredUsers.values()
                                                                                        .stream()
                                                                                        .noneMatch(u -> u.name()
                                                                                                        .equalsIgnoreCase(
                                                                                                                        data)),
                                                                        "This name is already taken."));

                        form.field("age")
                                        .trim()
                                        .required("Age is required.")
                                        .min(0, "Age must be a non-negative integer.")
                                        .min(18, "You must be at least 18 years old.");

                        // Combine type conversion and validation into one call.
                        form.field("dob")
                                        .trim()
                                        .required("Date of birth is required.")
                                        .date("yyyy-MM-dd"); // This method both registers the date pattern and
                                                             // validates.

                        form.field("email")
                                        .trim().toLowerCase()
                                        .required("Email is required.")
                                        .email("Invalid email format.")
                                        // Prevent duplicate emails.
                                        .addRules(
                                                        Rule.custom(
                                                                        (data) -> !registeredUsers
                                                                                        .containsKey(data),
                                                                        "This email is already registered."));

                        if (!form.verify()) {
                                StringBuilder sb = new StringBuilder();
                                sb.append("<html><head><title>Registration Error</title></head><body>");
                                sb.append("<h1>Registration Errors:</h1>");
                                form.getErrors().forEach((field, error) -> sb.append("<p><strong>").append(field)
                                                .append("</strong>: ")
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
                                                        "</html>")
                                        .status(HttpStatus.CREATED);
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

        private static void defineUploadRoutes() {

                // GET /upload: Display the file upload form
                get("/upload", ctx -> ctx.html(
                                "<html>" +
                                                "  <head><title>File Upload</title></head>" +
                                                "  <body>" +
                                                "    <h1>Upload an Image</h1>" +
                                                "    <form method='post' action='/upload' enctype='multipart/form-data'>"
                                                +
                                                "      <input type='file' name='imageFile'><br/><br/>"
                                                +
                                                "      <input type='submit' value='Upload'>" +
                                                "    </form>" +
                                                "  </body>" +
                                                "</html>"));

                // POST /upload: Handle the file upload
                post("/upload", ctx -> {
                        // Retrieve uploaded files using the updated getFiles() in the JoltHttpContext
                        var files = ctx.getFiles();
                        if (files.isEmpty()) {
                                return ctx.status(HttpStatus.BAD_REQUEST).html(
                                                "<html>" +
                                                                "  <head><title>Upload Failed</title></head>" +
                                                                "  <body>" +
                                                                "    <h1>No file was uploaded</h1>" +
                                                                "    <a href='/upload'>Try again</a>" +
                                                                "  </body>" +
                                                                "</html>");
                        }

                        // We'll assume a single file for this example
                        JoltFile file = files.get(0);

                        // Generate a unique ID for the file
                        String fileId = UUID.randomUUID().toString();

                        // Store the file in memory for later retrieval
                        byte[] fileData = file.getData();
                        String contentType = file.getContentType();
                        String fileName = file.getFileName();

                        uploadedFiles.put(fileId, file);

                        // Return a success page
                        return ctx.html(
                                        "<html>" +
                                                        "  <head><title>Upload Successful</title></head>" +
                                                        "  <body>" +
                                                        "    <h1>File Uploaded Successfully</h1>" +
                                                        "    <p>File: " + fileName + "</p>" +
                                                        "    <p>Type: " + contentType + "</p>" +
                                                        "    <p>Size: " + fileData.length + " bytes</p>" +
                                                        "    <p>ID: " + fileId + "</p>" +
                                                        "    <p><a href='/view/" + fileId + "'>View the file</a></p>" +
                                                        "    <p><a href='/image/" + fileId
                                                        + "'>Direct image link</a></p>" +
                                                        "    <p><a href='/upload'>Upload another file</a></p>" +
                                                        "  </body>" +
                                                        "</html>");
                });

                // GET /view/{fileId}: View the file in an HTML page
                get("/view/{fileId}", ctx -> {
                        String fileId = ctx.path("fileId").get();
                        JoltFile file = uploadedFiles.get(fileId);
                        if (file == null) {
                                return ctx.status(HttpStatus.NOT_FOUND).html(
                                                "<html>" +
                                                                "  <head><title>File Not Found</title></head>" +
                                                                "  <body>" +
                                                                "    <h1>File Not Found</h1>" +
                                                                "    <p>The requested file does not exist or has been removed.</p>"
                                                                +
                                                                "    <a href='/upload'>Upload a new file</a>" +
                                                                "  </body>" +
                                                                "</html>");
                        }

                        // If it's an image, embed it inline
                        if (file.getContentType().startsWith("image/")) {
                                String base64 = Base64.getEncoder().encodeToString(file.getData());
                                return ctx.html(
                                                "<html>" +
                                                                "  <head><title>View Image</title></head>" +
                                                                "  <body>" +
                                                                "    <h1>Image: " + file.getFileName() + "</h1>" +
                                                                "    <img src='data:" + file.getContentType()
                                                                + ";base64,"
                                                                + base64
                                                                + "' alt='" + file.getFileName() + "' />" +
                                                                "    <p><a href='/upload'>Upload another file</a></p>" +
                                                                "  </body>" +
                                                                "</html>");
                        } else {
                                // For non-image files, show some info and link to direct download
                                return ctx.html(
                                                "<html>" +
                                                                "  <head><title>File Details</title></head>" +
                                                                "  <body>" +
                                                                "    <h1>File: " + file.getFileName() + "</h1>" +
                                                                "    <p>Type: " + file.getContentType() + "</p>" +
                                                                "    <p>Size: " + file.getData().length + " bytes</p>" +
                                                                "    <p><a href='/image/" + fileId
                                                                + "'>Download file</a></p>" +
                                                                "    <p><a href='/upload'>Upload another file</a></p>" +
                                                                "  </body>" +
                                                                "</html>");
                        }
                });

                // GET /image/{fileId}: Serve the raw file with correct content type
                get("/image/{fileId}", ctx -> {
                        String fileId = ctx.path("fileId").get();
                        JoltFile file = uploadedFiles.get(fileId);
                        if (file == null) {
                                return ctx.status(HttpStatus.NOT_FOUND).text("File not found");
                        }

                        ctx.header("Content-Type", file.getContentType());
                        ctx.header("Content-Disposition", "inline; filename=\"" + file.getFileName() + "\"");
                        try {
                                ctx.getResponse().getOutputStream().write(file.getData());
                                return ctx;
                        } catch (IOException e) {
                                return ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .text("Error serving file: " + e.getMessage());
                        }
                });

                // GET /files: List all uploaded files
                get("/files", ctx -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append("<html><head><title>Uploaded Files</title></head><body>");
                        sb.append("<h1>Uploaded Files</h1>");

                        if (uploadedFiles.isEmpty()) {
                                sb.append("<p>No files have been uploaded yet.</p>");
                        } else {
                                sb.append("<ul>");
                                uploadedFiles.forEach((id, file) -> {
                                        sb.append("<li>")
                                                        .append("<strong>").append(file.getFileName())
                                                        .append("</strong> ")
                                                        .append("(").append(file.getContentType()).append(", ")
                                                        .append(file.getData().length).append(" bytes) - ")
                                                        .append("<a href='/view/").append(id).append("'>View</a> | ")
                                                        .append("<a href='/image/").append(id)
                                                        .append("'>Direct Link</a>")
                                                        .append("</li>");
                                });
                                sb.append("</ul>");
                        }

                        sb.append("<p><a href='/upload'>Upload a new file</a></p>");
                        sb.append("</body></html>");

                        return ctx.html(sb.toString());
                });
        }

        private void defineWeatherRoutes() {
                // Basic route that returns weather for a city in JSON format
                get("/weather", ctx -> {
                        String city = ctx.query("city").orDefault("London");
                        try {
                                Response response = Http.get(WEATHER_API_URL + "/" + city)
                                                .secure(true)
                                                .header("Accept", "application/json")
                                                .header("User-Agent", "Jolt Weather Service")
                                                .query("format", "j1") // Request JSON format
                                                .execute();

                                if (response.isSuccessful()) {
                                        return ctx.json(response.text());
                                } else {
                                        return ctx.json(createErrorResponse(response))
                                                        .status(response.status());
                                }
                        } catch (Exception e) {
                                return ctx.json(createExceptionResponse(e))
                                                .status(500);
                        }
                });

                // Enhanced route with path parameter and more detailed response formatting
                get("/weather/{city}", ctx -> {
                        String city = ctx.path("city").get();

                        try {
                                Response response = Http.get(WEATHER_API_URL + "/" + city)
                                                .secure(true)
                                                .query("format", "j1") // Request JSON format
                                                .execute();

                                if (response.isSuccessful()) {
                                        // Format a cleaner, more user-friendly response
                                        return ctx.json(formatWeatherResponse(response))
                                                        .status(200);
                                } else {
                                        // Create user-friendly error message
                                        ObjectMapper mapper = new ObjectMapper();
                                        ObjectNode errorNode = mapper.createObjectNode();

                                        errorNode.put("error", "Weather service error");
                                        errorNode.put("message", "Failed to get weather for '" + city + "'");
                                        errorNode.put("status", response.status());

                                        return ctx.json(errorNode.toString())
                                                        .status(response.status());
                                }
                        } catch (Exception e) {
                                // Handle exceptions
                                return ctx.json(createExceptionResponse(e))
                                                .status(500);
                        }
                });
        }

        /**
         * Formats the weather API response into a more user-friendly format.
         */
        private static String formatWeatherResponse(Response response) throws IOException {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode weatherData = mapper.readTree(response.text());

                ObjectNode formattedResponse = mapper.createObjectNode();

                // Extract location information
                String locationName = weatherData.path("nearest_area").path(0).path("areaName").path(0).path("value")
                                .asText();
                String country = weatherData.path("nearest_area").path(0).path("country").path(0).path("value")
                                .asText();
                formattedResponse.put("location", locationName);
                formattedResponse.put("country", country);

                // Current weather conditions
                JsonNode current = weatherData.path("current_condition").path(0);
                ObjectNode currentWeather = formattedResponse.putObject("current");
                currentWeather.put("temperature_c", current.path("temp_C").asInt());
                currentWeather.put("temperature_f", current.path("temp_F").asInt());
                currentWeather.put("description", current.path("weatherDesc").path(0).path("value").asText());
                currentWeather.put("humidity", current.path("humidity").asInt());
                currentWeather.put("wind_speed_kmph", current.path("windspeedKmph").asInt());
                currentWeather.put("wind_direction", current.path("winddir16Point").asText());
                currentWeather.put("feels_like_c", current.path("FeelsLikeC").asInt());

                // Get forecast for next few days
                ObjectNode forecast = formattedResponse.putObject("forecast");
                JsonNode weatherForecast = weatherData.path("weather");

                for (int i = 0; i < weatherForecast.size(); i++) {
                        JsonNode day = weatherForecast.path(i);
                        String date = day.path("date").asText();

                        ObjectNode dayForecast = forecast.putObject(date);
                        dayForecast.put("max_temp_c", day.path("maxtempC").asInt());
                        dayForecast.put("min_temp_c", day.path("mintempC").asInt());
                        dayForecast.put("sunrise", day.path("astronomy").path(0).path("sunrise").asText());
                        dayForecast.put("sunset", day.path("astronomy").path(0).path("sunset").asText());

                        // Add hourly forecast summary (just one point for simplicity)
                        JsonNode hourly = day.path("hourly").path(1); // Midday forecast
                        dayForecast.put("description", hourly.path("weatherDesc").path(0).path("value").asText());
                        dayForecast.put("chance_of_rain", hourly.path("chanceofrain").asText() + "%");
                }

                return mapper.writeValueAsString(formattedResponse);
        }

        /**
         * Creates an error response from an HTTP response.
         */
        private static String createErrorResponse(Response response) throws IOException {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode errorNode = mapper.createObjectNode();

                errorNode.put("error", "Weather service error");
                errorNode.put("status", response.status());
                errorNode.put("message", response.text());

                return mapper.writeValueAsString(errorNode);
        }

        /**
         * Creates an error response from an exception.
         */
        private static String createExceptionResponse(Exception e) {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode errorNode = mapper.createObjectNode();

                errorNode.put("error", "Internal server error");
                errorNode.put("message", e.getMessage());

                try {
                        return mapper.writeValueAsString(errorNode);
                } catch (JsonProcessingException e1) {
                        // If we can't serialize the error, just return a plain string
                }
                return "";
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
