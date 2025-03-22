package ca.jolt;

import ca.jolt.database.RestBroker;

public class ProductBroker extends RestBroker<Integer, Product> {

    protected ProductBroker() {
        super("product", Product.class, int.class);
    }
}
