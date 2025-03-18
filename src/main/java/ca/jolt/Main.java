
package ca.jolt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ca.jolt.core.JoltApplication;
import ca.jolt.database.Broker;
import ca.jolt.database.annotation.CascadeType;
import ca.jolt.database.annotation.FetchType;
import ca.jolt.database.annotation.Id;
import ca.jolt.database.annotation.JoinColumn;
import ca.jolt.database.annotation.JoinTable;
import ca.jolt.database.annotation.ManyToMany;
import ca.jolt.database.annotation.Table;
import ca.jolt.form.Form;
import ca.jolt.http.HttpStatus;
import ca.jolt.routing.context.JoltContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class Main extends JoltApplication {
    public static void main(String[] args) {
        launch(Main.class, "ca.jolt");
    }

    @Override
    public void setup() {
        get("/", Main::getUser);
        group("/user", () -> {
            get("/{name}", ctx -> ctx.json(new UserBroker().findByName(ctx.path("name").get()).get()));
            get("", ctx -> ctx.json(new UserBroker().findAll()));
            post("", ctx -> {
                Form form = ctx.buildForm();
                User user = form.buildEntity(User.class);
                new UserBroker().save(user);
                return ctx.created().json(user);
            });
            put("/{user_id:int}/{product_id:int}", ctx -> {
                int userId = ctx.path("user_id").asInt();
                int productId = ctx.path("product_id").asInt();
                User user = new UserBroker().findById(userId).get();
                Product product = new ProductBroker().findById(productId).get();
                user.getProducts().add(product);
                new UserBroker().save(user);
                return ctx.ok().json(user);
            });
            put("/{id:int}", ctx -> {
                Form form = ctx.buildForm();

                form.field("name")
                        .required()
                        .minLength(3);
                if (!form.verify()) {
                    return ctx.status(HttpStatus.BAD_REQUEST).json(form.getErrors());
                }

                User user = new UserBroker().findById(ctx.path("id").asInt()).get();
                new UserBroker().save(form.updateEntity(user));
                return ctx.ok().json(Map.of("Message", "User update"));
            });
        });
        group("/product", () -> {
            get("", ProductController::getProducts);
            get("/{id:int}", ProductController::getProduct);
            post("", ProductController::createProduct);
            put("/{id:int}", ProductController::updateProduct);
            delete("/{id:int}", ProductController::deleteProduct);
        });
    }

    public static JoltContext getUser(JoltContext ctx) {
        return ctx.json(Map.of("name", "John Doe", "age", 30));
    }

    public static class UserBroker extends Broker<Integer, User> {
        public Optional<User> findByName(String name) {
            return selectSingle("WHERE name = ?", name);
        }
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Table(table = "users")
    @Getter
    @Setter
    public static class User {
        @Id
        private Integer id;
        private String name;
        private String email;

        @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
        @JoinTable(name = "user_product_link", joinColumns = @JoinColumn(value = "user_id"), inverseJoinColumns = @JoinColumn(value = "product_id"))
        private List<Product> products = new ArrayList<>();
    }
}
