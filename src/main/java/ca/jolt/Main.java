package ca.jolt;

import java.util.Map;

import ca.jolt.core.JoltApplication;
import ca.jolt.form.Form;
import ca.jolt.http.HttpStatus;
import ca.jolt.routing.context.JoltContext;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class, "ca.jolt");
    }

    @Override
    public void setup() {
        get("/", Main::getUser);
        get("/login", ctx -> ctx.serve("login.html"));
        post("/login", ctx -> {
            Form form = ctx.buildForm();
            form.field("username")
                    .required();
            form.field("password")
                    .required();

            if (form.verify()) {
                if (form.getValue("username").equals("admin") && form.getValue("password").equals("password")) {
                    return ctx.html("You are logged in!").ok();
                } else {
                    return ctx.html("Wrong password or username!");
                }
            } else {
                return ctx.html("<h1>Error on form submission!</h1> <br> <p>" + form.getAllErrors() + "</p>")
                        .status(HttpStatus.BAD_REQUEST);
            }
        });
    }

    public static JoltContext getUser(JoltContext ctx) {
        return ctx.status(HttpStatus.OK).json(Map.of("name", "John Doe", "age", 30));
    }
}
/*
 * 
 * {
 * Form form = ctx.buildForm();
 * form.field("username")
 * .required();
 * form.field("password")
 * .required();
 * 
 * if (form.verify()) {
 * if (form.getValue("username").equals("admin") &&
 * form.getValue("password").equals("password")) {
 * ctx.html("<h1>Logged in!</h1>");
 * } else {
 * ctx.html("<h1>Invalid username or password!</h1>");
 * }
 * } else {
 * ctx.html("<h1>Invalid login!</h1>" + "<p>" + form.getAllErrors() + "</p>");
 * }
 * }
 * 
 */