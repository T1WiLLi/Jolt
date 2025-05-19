package io.github.t1willi;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.t1willi.annotations.Controller;
import io.github.t1willi.annotations.Get;
import io.github.t1willi.annotations.Path;
import io.github.t1willi.annotations.Query;
import io.github.t1willi.core.ApiController;
import io.github.t1willi.http.ResponseEntity;

@Controller("[controller]")
public class ProductController extends ApiController {

    private final List<Product> products = List.of(
            new Product(1L, "Wireless Mouse", "Electronics", 29.99),
            new Product(2L, "Mechanical Keyboard", "Electronics", 79.99),
            new Product(3L, "Water Bottle", "Outdoors", 15.49),
            new Product(4L, "Camping Tent", "Outdoors", 120.00),
            new Product(5L, "Notebook", "Stationery", 3.49),
            new Product(6L, "Pen Set", "Stationery", 5.99));

    public class Product {
        private final Long id;
        private final String name;
        private final String category;
        private final double price;

        public Product(Long id, String name, String category, double price) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.price = price;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getCategory() {
            return category;
        }

        public double getPrice() {
            return price;
        }
    }

    @Get
    public ResponseEntity<List<Product>> listAll(
            @Query("category") Optional<String> category,
            @Query("maxPrice") Optional<Double> maxPrice) {
        List<Product> filtered = products.stream()
                .filter(p -> category.map(c -> p.getCategory().equalsIgnoreCase(c)).orElse(true))
                .filter(p -> maxPrice.map(max -> p.getPrice() <= max).orElse(true))
                .collect(Collectors.toList());
        return ResponseEntity.ok(filtered);
    }

    @Get("/{id}")
    public ResponseEntity<?> getById(@Path("id") Long id) {
        return products.stream()
                .filter(p -> p.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound("Product with ID " + id + " not found"));
    }

    @Get("/search")
    public ResponseEntity<List<Product>> searchByName(
            @Query("name") String name) {
        String term = name.toLowerCase();
        List<Product> matches = products.stream()
                .filter(p -> p.getName().toLowerCase().contains(term))
                .collect(Collectors.toList());
        return ResponseEntity.ok(matches);
    }

    @Get("/categories")
    public ResponseEntity<Set<String>> listCategories() {
        Set<String> cats = products.stream()
                .map(Product::getCategory)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(cats);
    }
}
