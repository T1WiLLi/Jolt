package io.github.t1willi;

import io.github.t1willi.core.JoltApplication;
import io.github.t1willi.security.session.Session;

import java.time.Instant;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class);
    }

    @Override
    public void init() {
        get("/", ctx -> {
            String id = Session.getSessionId();
            String ip = Session.get(Session.KEY_IP_ADDRESS, "N/A");
            String ua = Session.get(Session.KEY_USER_AGENT, "N/A");
            String access = Session.getAccess();
            String expire = Session.getExpire();
            String created = Session.get("createdAt", "N/A");

            String html = String.format("""
                    <html><body>
                      <h1>Session Demo</h1>
                      <form action="/create" method="post">
                        <button>Create Session</button>
                      </form>
                      <form action="/destroy" method="post">
                        <button>Destroy Session</button>
                      </form>
                      <h2>Session Info</h2>
                      <ul>
                        <li><strong>ID:</strong> %s</li>
                        <li><strong>IP Address:</strong> %s</li>
                        <li><strong>User-Agent:</strong> %s</li>
                        <li><strong>Last Access:</strong> %s</li>
                        <li><strong>Expire At:</strong> %s</li>
                        <li><strong>Created At:</strong> %s</li>
                      </ul>
                    </body></html>
                    """, id, ip, ua, access, expire, created);

            return ctx.html(html);
        });

        post("/create", ctx -> {
            Session.set("createdAt", Instant.now().toString());
            return ctx.redirect("/");
        });

        post("/destroy", ctx -> {
            Session.destroy();
            return ctx.redirect("/");
        });
    }
}
