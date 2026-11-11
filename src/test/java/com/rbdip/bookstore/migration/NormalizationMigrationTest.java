package com.rbdip.bookstore.migration;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class NormalizationMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void preservesOrdersAndItemsWhenNormalizingAnExistingDatabase() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(postgres.getJdbcUrl());
        dataSource.setUser(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        migrateTo(dataSource, "1");
        Long matchingProductId = seedOrders(jdbc);
        var originalOrders = jdbc.queryForList("""
                SELECT id, customer_full_name AS full_name, customer_address AS address, customer_phone AS phone,
                       status, created_at
                FROM orders ORDER BY id
                """);
        var originalItems = jdbc.queryForList("""
                SELECT id, order_id, product_name, product_price, quantity FROM order_items ORDER BY id
                """);

        migrateTo(dataSource, "2");

        assertThat(jdbc.queryForList("""
                SELECT o.id, c.full_name, c.address, c.phone, o.status, o.created_at
                FROM orders o JOIN customers c ON c.id = o.customer_id ORDER BY o.id
                """)).containsExactlyElementsOf(originalOrders);
        var customerIds = jdbc.queryForList("SELECT customer_id FROM orders ORDER BY id", Long.class);
        assertThat(customerIds).hasSize(3);
        assertThat(customerIds.getFirst()).isEqualTo(customerIds.get(1)).isNotEqualTo(customerIds.getLast());

        assertThat(jdbc.queryForList("""
                SELECT i.id, i.order_id, p.name AS product_name, p.price AS product_price, i.quantity
                FROM order_items i JOIN products p ON p.id = i.product_id ORDER BY i.id
                """)).containsExactlyElementsOf(originalItems);
        var productIds = jdbc.queryForList("SELECT product_id FROM order_items ORDER BY id", Long.class);
        assertThat(productIds).hasSize(2).first().isEqualTo(matchingProductId);
    }

    private static void migrateTo(PGSimpleDataSource dataSource, String version) {
        Flyway.configure().dataSource(dataSource).target(version).load().migrate();
    }

    private static Long seedOrders(JdbcTemplate jdbc) {
        jdbc.update("INSERT INTO products (name, price) VALUES ('Book', 99.00)");
        Long matchingProductId = jdbc.queryForObject(
                "INSERT INTO products (name, price) VALUES ('Book', 12.50) RETURNING id", Long.class);
        jdbc.update("""
                INSERT INTO orders (id, customer_full_name, customer_address, customer_phone, status, created_at)
                VALUES (10, 'Ada Lovelace', 'First Street', NULL, 'paid', '2024-02-03 10:00:00'),
                       (11, 'Ada Lovelace', 'First Street', NULL, 'new', '2024-03-04 11:00:00'),
                       (12, 'Ada Lovelace', 'Second Street', NULL, 'shipped', '2024-04-05 12:00:00')
                """);
        jdbc.update("""
                INSERT INTO order_items (order_id, product_name, product_price, quantity)
                VALUES (10, 'Book', 12.50, 3), (12, 'Legacy book', 8.75, 2)
                """);
        return matchingProductId;
    }
}
