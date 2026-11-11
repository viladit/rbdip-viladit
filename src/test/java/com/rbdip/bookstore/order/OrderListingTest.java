package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.rbdip.bookstore.reference.AbstractIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

class OrderListingTest extends AbstractIntegrationTest {

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void listsRepeatedProductsAndOrdersWithoutItemsInOneQuery() {
        long productId = jdbcTemplate.queryForObject(
                "INSERT INTO products (name, price) VALUES ('Refactoring', 40) RETURNING id", Long.class);
        var request = new CreateOrderRequest("Anna Sidorova", "Test Address", null, null, null,
                List.of(new CreateOrderRequest.Item(productId, 2), new CreateOrderRequest.Item(productId, null)));
        var created = restTemplate.postForEntity("/orders", request, JsonNode.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        long emptyOrderId = jdbcTemplate.queryForObject("""
                INSERT INTO orders (customer_id) SELECT id FROM customers RETURNING id
                """, Long.class);
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var response = restTemplate.getForEntity("/orders", JsonNode.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode orders = response.getBody();
        assertThat(orders).hasSize(2);
        JsonNode firstOrder = orders.get(0);
        assertThat(firstOrder.required("id")).isEqualTo(created.getBody().required("id"));
        assertThat(firstOrder.required("customerFullName").textValue()).isEqualTo("Anna Sidorova");
        JsonNode items = firstOrder.required("items");
        assertThat(items).hasSize(2);
        assertThat(items.get(0).required("productName").textValue()).isEqualTo("Refactoring");
        assertThat(items.get(0).required("quantity").intValue()).isEqualTo(2);
        assertThat(items.get(1).required("quantity").intValue()).isEqualTo(1);
        assertThat(orders.get(1).required("id").longValue()).isEqualTo(emptyOrderId);
        assertThat(orders.get(1).required("items")).isEmpty();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }
}
