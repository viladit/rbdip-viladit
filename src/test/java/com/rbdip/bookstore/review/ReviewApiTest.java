package com.rbdip.bookstore.review;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.rbdip.bookstore.reference.AbstractIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ReviewApiTest extends AbstractIntegrationTest {

    @Test
    void acceptsAnonymousReviewsWithoutRecordedPurchases() {
        long productId = jdbcTemplate.queryForObject(
                "INSERT INTO products (name, price) VALUES ('Refactoring', 40) RETURNING id", Long.class);
        String endpoint = "/products/" + productId + "/reviews";

        var created = restTemplate.postForEntity(endpoint, Map.of("rating", 4), JsonNode.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        var listed = restTemplate.getForEntity(endpoint, JsonNode.class);
        assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listed.getBody()).singleElement().satisfies(review -> {
            assertThat(review.required("authorName").textValue()).isEqualTo("anonymous");
            assertThat(review.required("rating").intValue()).isEqualTo(4);
            assertThat(review.required("comment").textValue()).isEmpty();
        });
    }
}
