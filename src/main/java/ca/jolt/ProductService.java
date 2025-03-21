package ca.jolt;

import java.util.List;
import java.util.Optional;

import ca.jolt.exceptions.JoltHttpException;
import ca.jolt.http.HttpStatus;

public class ProductService {

    public List<Product> getProducts() {
        return new ProductBroker().findAll();
    }

    public Product getProduct(int id) {
        Optional<Product> product = new ProductBroker().findById(id);
        return product.orElseThrow(() -> new JoltHttpException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    public Product createProduct(Product product) {
        return new ProductBroker().save(product);
    }

    public Product updateProduct(Product product) {
        return new ProductBroker().save(product);
    }

    public boolean deleteProduct(int id) {
        return new ProductBroker().deleteById(id);
    }
}
