package ca.jolt;

import ca.jolt.form.Form;
import ca.jolt.routing.context.JoltContext;

public class ProductController {

    private static final ProductService productService = new ProductService();

    public static JoltContext getAll(JoltContext ctx) {
        return ctx.json(productService.getProducts()).ok();
    }

    public static JoltContext get(JoltContext ctx) {
        return ctx.json(productService.getProduct(ctx.path("id").asInt())).ok();
    }

    public static JoltContext create(JoltContext ctx) {
        Form form = ctx.buildForm();
        if (ProductValidator.validate(form).verify()) {
            return ctx.json(productService.createProduct(form.buildEntity(Product.class))).created();
        } else {
            return ctx.json(form.getErrors());
        }
    }

    public static JoltContext update(JoltContext ctx) {
        Form form = ctx.buildForm();
        Product product = productService.getProduct(ctx.path("id").asInt());
        product = productService.updateProduct(form.updateEntity(product));
        return ctx.json(product).ok();
    }

    public static JoltContext delete(JoltContext ctx) {
        boolean isDeleted = productService.deleteProduct(ctx.path("id").asInt());
        if (isDeleted) {
            return ctx.noContent();
        } else {
            return ctx.abortNotFound("Product was not found");
        }
    }
}
