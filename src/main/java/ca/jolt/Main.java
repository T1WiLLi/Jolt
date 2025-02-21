package ca.jolt;

import ca.jolt.core.JoltApplication;
import ca.jolt.core.Router;
import ca.jolt.tomcat.abstraction.WebServer;

public class Main extends JoltApplication {
    public static void main(String[] args) throws Exception {
        Main main = new Main();

        main.useCustomError(false);
        main.useCustomNotFound(false);

        buildServer()
                .withPort(8080);

        get("/", () -> "Hello, World!");
        get("/hello/{age}", (ctx) -> ctx.html("Hello " + ctx.query("name").orDefault("little one") + ", you are "
                + ctx.path("age").orDefault(String.valueOf(Integer.MIN_VALUE)) + " years old!"));
        post("/user", (ctx) -> {
            User user = ctx.body(User.class);
            return ctx.html("Hello, " + user.name + "!");
        });

        setupSecret();
        start();
    }

    private static class User {
        public String name;
        public int age;
    }

    @Override
    protected void configureRouting(WebServer server, Router router) {
        server.setRouter(router);
    }

    private static void setupSecret() {
        get("/hello", (ctx) -> {
            String name = ctx.query("name").orDefault("World!");

            // Build the sparkle elements dynamically (50 sparkles)
            StringBuilder sparklesBuilder = new StringBuilder();
            for (int i = 0; i < 50; i++) {
                double left = Math.random() * 100;
                double top = Math.random() * 100;
                double delay = Math.random() * 2;
                sparklesBuilder.append("<div class='sparkle' style='left:")
                        .append(left)
                        .append("%%; top:")
                        .append(top)
                        .append("%%; animation-delay:")
                        .append(delay)
                        .append("s;'></div>");
            }
            String sparkles = sparklesBuilder.toString();
            int visitorNumber = (int) (Math.random() * 10000);
            String html = String.format(
                    """
                            <!DOCTYPE html>
                            <html lang="en">
                            <head>
                                <meta charset="UTF-8">
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <title>Greetings for %s</title>
                                <style>
                                    @keyframes float {
                                        0%% { transform: translateY(0px) rotate(0deg); }
                                        50%% { transform: translateY(-20px) rotate(5deg); }
                                        100%% { transform: translateY(0px) rotate(0deg); }
                                    }

                                    body {
                                        background: linear-gradient(135deg, #1a1a1a, #4a4a4a);
                                        height: 100vh;
                                        margin: 0;
                                        display: flex;
                                        justify-content: center;
                                        align-items: center;
                                        font-family: 'Comic Sans MS', cursive;
                                        overflow: hidden;
                                    }

                                    .container {
                                        background: rgba(255, 255, 255, 0.95);
                                        padding: 2rem;
                                        border-radius: 20px;
                                        box-shadow: 0 0 50px rgba(0,0,0,0.3);
                                        position: relative;
                                        transform: rotate(-1deg);
                                    }

                                    .greeting {
                                        font-size: 3rem;
                                        color: #ff6b6b;
                                        text-shadow: 2px 2px 0 #ff9f43;
                                        animation: float 3s ease-in-out infinite;
                                    }

                                    .sparkles {
                                        position: absolute;
                                        width: 100%%;
                                        height: 100%%;
                                        pointer-events: none;
                                    }

                                    .sparkle {
                                        position: absolute;
                                        background: radial-gradient(circle, #fff 20%%, transparent 80%%);
                                        width: 10px;
                                        height: 10px;
                                        animation: sparkle 1.5s linear infinite;
                                    }

                                    .duck {
                                        position: fixed;
                                        bottom: -50px;
                                        right: -50px;
                                        font-size: 100px;
                                        animation: float 2s ease-in-out infinite;
                                        transform: rotate(-45deg);
                                        opacity: 0.3;
                                    }

                                    @keyframes sparkle {
                                        0%% { transform: scale(0); opacity: 1; }
                                        100%% { transform: scale(3); opacity: 0; }
                                    }

                                    .warning {
                                        position: fixed;
                                        bottom: 10px;
                                        right: 10px;
                                        font-size: 8px;
                                        color: #666;
                                        transform: rotate(3deg);
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="sparkles">
                                    %s
                                </div>

                                <div class="container">
                                    <h1 class="greeting">✨ Hello, %s! ✨</h1>
                                    <p>You are visitor #%d</p>
                                    <marquee behavior="alternate" scrollamount="15">🎉 CONGRATULATIONS! 🎉</marquee>
                                </div>

                                <div class="duck">🦆</div>

                                <div class="warning">
                                    This page contains 99%% pure CSS awesome. May cause dizziness.
                                </div>
                            </body>
                            </html>
                            """, name, sparkles, name, visitorNumber);

            return ctx.html(html);
        });
    }
}