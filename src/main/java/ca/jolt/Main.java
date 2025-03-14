package ca.jolt;

import java.util.Map;

import ca.jolt.core.JoltApplication;
import ca.jolt.database.Broker;
import ca.jolt.database.Database;
import ca.jolt.database.annotation.Id;
import ca.jolt.database.annotation.Table;
import ca.jolt.form.Form;
import ca.jolt.http.HttpStatus;
import ca.jolt.routing.context.JoltContext;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class, "ca.jolt");
    }

    @Override
    public void setup() {
        Database.init();
        get("/", Main::getUser);
        group("/user", () -> {
            get("", ctx -> ctx.json(new UserBroker().findAll()));
            post("", ctx -> {
                Form form = ctx.buildForm();
                User user = form.buildEntity(User.class);
                new UserBroker().save(user);
                return ctx.created().json(user);
            });
            put("/{id:int}", ctx -> {
                Form form = ctx.buildForm();

                form.field("name")
                        .required()
                        .minLength(3);
                if (!form.verify()) {
                }

                User user = new UserBroker().findById(ctx.path("id").asInt()).get();
                new UserBroker().save(form.updateEntity(user));
                return ctx.ok().json(Map.of("Message", "User update"));
            });
        });
    }

    public static JoltContext getUser(JoltContext ctx) {
        return ctx.status(HttpStatus.OK).json(Map.of("name", "John Doe", "age", 30));
    }

    public static class UserBroker extends Broker<Integer, User> {

    }

    @NoArgsConstructor
    @Table(table = "users")
    @Getter
    @Setter
    public static class User {
        @Id
        private Integer id;

        private String name;
        private String email;

        public User(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }
}
