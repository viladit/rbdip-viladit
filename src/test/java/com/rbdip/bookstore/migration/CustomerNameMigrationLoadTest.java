package com.rbdip.bookstore.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.JsonNode;
import com.rbdip.bookstore.BookstoreApplication;
import com.rbdip.bookstore.order.CreateOrderRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CustomerNameMigrationLoadTest {

    private static final int READERS = 3;
    private static final List<String> CONCURRENT_CUSTOMERS = List.of("First Customer", "Second Customer");
    private static final String PREEXISTING_NAME = "  Anna  Sidorova ";
    private static final String LEGACY_WRITER_NAME = "Plato";
    private static final String NEW_WRITER_NAME = "  Иван  Петров ";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void servesOrderRequestsWhileContractMigrationIsRunning() throws Exception {
        var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        var jdbc = new JdbcTemplate(dataSource);

        migrate(dataSource, "2");
        long productId = jdbc.queryForObject(
                "INSERT INTO products (name, price) VALUES ('Refactoring', 40.00) RETURNING id", Long.class);
        seedLegacyOrder(jdbc, PREEXISTING_NAME, productId);
        migrate(dataSource, "4");
        seedLegacyOrder(jdbc, LEGACY_WRITER_NAME, productId);

        try (var application = new SpringApplicationBuilder(BookstoreApplication.class).run(
                "--server.port=0",
                "--spring.flyway.enabled=false",
                "--spring.jpa.properties.hibernate.generate_statistics=false",
                "--spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                "--spring.datasource.username=" + POSTGRES.getUsername(),
                "--spring.datasource.password=" + POSTGRES.getPassword())) {
            int port = ((WebServerApplicationContext) application).getWebServer().getPort();
            RestTemplate api = new RestTemplateBuilder()
                    .rootUri("http://localhost:" + port)
                    .setConnectTimeout(Duration.ofSeconds(5))
                    .setReadTimeout(Duration.ofSeconds(15))
                    .build();

            long newOrderId = postOrder(api, NEW_WRITER_NAME, productId);
            String legacyRead = jdbc.queryForObject("""
                    SELECT c.full_name FROM customers c JOIN orders o ON o.customer_id = c.id WHERE o.id = ?
                    """, String.class, newOrderId);
            assertThat(legacyRead).isEqualTo(NEW_WRITER_NAME);
            assertThat(listOrderNames(api)).containsExactlyInAnyOrder(
                    PREEXISTING_NAME, LEGACY_WRITER_NAME, NEW_WRITER_NAME);

            migrateUnderLoad(dataSource, jdbc, api, productId);

            assertThat(listOrderNames(api)).containsExactlyInAnyOrder(
                    PREEXISTING_NAME, LEGACY_WRITER_NAME, NEW_WRITER_NAME,
                    CONCURRENT_CUSTOMERS.get(0), CONCURRENT_CUSTOMERS.get(1));
            postOrder(api, "After Contract", productId);
            assertThat(listOrderNames(api)).contains("After Contract");
        }
    }

    private static void migrateUnderLoad(PGSimpleDataSource dataSource, JdbcTemplate jdbc,
            RestTemplate api, long productId) throws Exception {
        int requests = READERS + CONCURRENT_CUSTOMERS.size();
        try (var workers = Executors.newFixedThreadPool(requests + 1)) {
            Future<?> contract;
            List<Future<List<String>>> readers = new ArrayList<>();
            List<Future<Long>> writers = new ArrayList<>();
            try (var blocker = dataSource.getConnection()) {
                blocker.setAutoCommit(false);
                try (var statement = blocker.createStatement()) {
                    statement.execute("LOCK TABLE customers IN ACCESS SHARE MODE");
                }

                contract = workers.submit(() -> migrate(dataSource, "5"));
                awaitBlockedSessions(jdbc, true, 1);
                for (int i = 0; i < READERS; i++) {
                    readers.add(workers.submit(() -> listOrderNames(api)));
                }
                for (String name : CONCURRENT_CUSTOMERS) {
                    writers.add(workers.submit(() -> postOrder(api, name, productId)));
                }
                awaitBlockedSessions(jdbc, false, requests);
                blocker.commit();
            }

            contract.get(15, TimeUnit.SECONDS);
            for (var reader : readers) {
                assertThat(reader.get(15, TimeUnit.SECONDS)).contains(PREEXISTING_NAME, LEGACY_WRITER_NAME);
            }
            for (var writer : writers) {
                assertThat(writer.get(15, TimeUnit.SECONDS)).isPositive();
            }
        }
    }

    private static void awaitBlockedSessions(JdbcTemplate jdbc, boolean migration, int count) {
        await().atMost(Duration.ofSeconds(5)).until(() -> jdbc.queryForObject("""
                SELECT count(DISTINCT pid)
                FROM pg_locks
                WHERE relation = 'customers'::regclass
                  AND NOT granted
                  AND (mode = 'AccessExclusiveLock') = ?
                """, Integer.class, migration) >= count);
    }

    private static void seedLegacyOrder(JdbcTemplate jdbc, String fullName, long productId) {
        long customerId = jdbc.queryForObject("""
                INSERT INTO customers (full_name, address) VALUES (?, 'Test Address') RETURNING id
                """, Long.class, fullName);
        long orderId = jdbc.queryForObject(
                "INSERT INTO orders (customer_id) VALUES (?) RETURNING id", Long.class, customerId);
        jdbc.update("INSERT INTO order_items (order_id, product_id, quantity) VALUES (?, ?, 1)", orderId, productId);
    }

    private static long postOrder(RestTemplate api, String name, long productId) {
        var request = new CreateOrderRequest(name, "Test Address", null, "regular", null,
                List.of(new CreateOrderRequest.Item(productId, 1)));
        var response = api.postForEntity("/orders", request, JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().required("id").longValue();
    }

    private static List<String> listOrderNames(RestTemplate api) {
        var response = api.getForEntity("/orders", JsonNode.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> names = new ArrayList<>();
        for (JsonNode order : response.getBody()) {
            names.add(order.required("customerFullName").textValue());
        }
        return names;
    }

    private static void migrate(PGSimpleDataSource dataSource, String target) {
        Flyway.configure().dataSource(dataSource).target(target).load().migrate();
    }
}
