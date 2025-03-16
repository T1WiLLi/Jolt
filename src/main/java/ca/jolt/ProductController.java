package ca.jolt;

import java.util.Map;

import ca.jolt.form.Form;
import ca.jolt.routing.context.JoltContext;

public class ProductController {

    public static JoltContext getProducts(JoltContext context) {
        return context.json(new ProductBroker().findAll());
    }

    public static JoltContext getProduct(JoltContext context) {
        return context.json(new ProductBroker().findById(context.path("id").asInt()));
    }

    public static JoltContext createProduct(JoltContext context) {
        Form form = context.buildForm();
        Product product = form.buildEntity(Product.class);
        new ProductBroker().save(product);
        return context.json(product);
    }

    public static JoltContext updateProduct(JoltContext context) {
        Form form = context.buildForm();
        Product product = new ProductBroker().findById(context.path("id").asInt())
                .orElseThrow(() -> new RuntimeException("Product not found"));
        product = form.updateEntity(product);
        new ProductBroker().save(product);
        return context.json(product);
    }

    public static JoltContext deleteProduct(JoltContext context) {
        int id = context.path("id").asInt();
        return context.json((new ProductBroker().deleteById(id) ? Map.of("message", "Product deleted")
                : Map.of("message", "Product not found")));
    }
}
