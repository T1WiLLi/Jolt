package io.github.t1willi;

import java.util.Enumeration;

import io.github.t1willi.core.JoltApplication;
import io.github.t1willi.security.session.Session;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    public void init() {
        get("/", ctx -> {
            Enumeration<String> names = Session.raw().getAttributeNames();

            StringBuilder sb = new StringBuilder();
            sb.append("<h1>Session Data</h1>")
                    .append("<ul>");

            while (names.hasMoreElements()) {
                String key = names.nextElement();
                Object value = Session.get(key, null);
                sb.append("<li>")
                        .append(key).append(": ").append(value)
                        .append("</li>");
            }

            sb.append("</ul>");
            return ctx.html(sb.toString());
        });
        get("/session", ctx -> {
            Session.set("username", "t1willi");
            return ctx.redirect("/");
        });
        get("/sessionoff", ctx -> {
            Session.destroy();
            return ctx.redirect("/");
        });
    }
}
