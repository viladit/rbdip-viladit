package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class OrderRequestPreparer {

    private static final int DEFAULT_QUANTITY = 1;

    private final ProductRepository productRepository;

    OrderRequestPreparer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    List<OrderLine> prepare(CreateOrderRequest request) {
        requireNonBlank(request.customerFullName(), "customerFullName");
        requireNonBlank(request.customerAddress(), "customerAddress");
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }

        List<OrderLine> lines = new ArrayList<>();
        for (CreateOrderRequest.Item item : request.items()) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException("product " + item.productId() + " not found"));
            int quantity = item.quantity() == null ? DEFAULT_QUANTITY : item.quantity();
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
            lines.add(new OrderLine(product, quantity));
        }
        return lines;
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
