package io.github.t1willi;

import io.github.t1willi.core.JoltApplication;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    public void init() {
        get("/", ctx -> {
            String query = ctx.query("search").orDefault("No search query");
            StringBuilder sb = new StringBuilder();
            sb.append("<form action=\"/\" method=\"get\">");
            sb.append("<input name=\"search\" value=\"").append(query).append("\"></input>");
            sb.append("<button type=\"submit\">Search</button>");
            sb.append("</form>");
            sb.append("<div>Search query: ").append(query).append("</div>");
            return ctx.html(sb.toString());
        });
    }
}
