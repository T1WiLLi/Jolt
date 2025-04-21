package io.github.t1willi;

import java.util.LinkedHashSet;
import java.util.Set;

import io.github.t1willi.core.JoltApplication;
import io.github.t1willi.security.role.DefaultRoles;
import io.github.t1willi.security.role.Role;
import io.github.t1willi.security.role.Roles;

public class Main extends JoltApplication {

    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    protected void init() {
        get("/", ctx -> ctx.html("Hello, World!"));
        get("/roles", ctx -> {
            StringBuilder rolesList = new StringBuilder("<ul>");
            rolesList.append("<li>").append(DefaultRoles.GUEST.toString()).append("</li>");
            rolesList.append("<li>").append(DefaultRoles.USER.toString()).append("</li>");
            rolesList.append("<li>").append(DefaultRoles.MODERATOR.toString()).append("</li>");
            rolesList.append("<li>").append(DefaultRoles.ADMIN.toString()).append("</li>");
            rolesList.append("<li>").append(DefaultRoles.SUPER_ADMIN.toString()).append("</li>");
            rolesList.append("<li>").append(DefaultRoles.ROOT.toString()).append("</li>");
            rolesList.append("</ul>");
            return ctx.html(rolesList.toString());
        });
        get("/roles/{role}", ctx -> {
            Role role = Roles.of(ctx.path("role"));

            if (role == null) {
                return ctx.html("<h1>Role not found:</h1>"
                        + "<p style=\"color:red;\">" + ctx.path("role") + "</p>").status(404);
            }

            StringBuilder html = new StringBuilder();
            html.append("<h1>Role: ").append(role.name()).append("</h1>");

            Set<Role> parents = new LinkedHashSet<>(Roles.parents(role));
            parents.remove(role);

            html.append("<h2>Inherited Roles</h2>")
                    .append("<ul>");
            if (parents.isEmpty()) {
                html.append("<li><em>none</em></li>");
            } else {
                for (Role p : parents) {
                    html.append("<li>").append(p.name()).append("</li>");
                }
            }
            html.append("</ul>");

            return ctx.html(html.toString());
        });
    }
}
