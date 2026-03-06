package com.sourabh.product_service;

import com.sourabh.product_service.search.repository.ProductSearchRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test that verifies the Product Service application context
 * loads successfully with all required beans.
 *
 * <p>Uses Testcontainers to spin up a real PostgreSQL instance and mocks
 * the Elasticsearch layer (repository + operations) since an ES container
 * is not needed for basic context verification. Kafka auto-startup is
 * disabled to avoid broker connectivity requirements during tests.</p>
 *
 * @see org.springframework.boot.test.context.SpringBootTest
 * @see org.testcontainers.junit.jupiter.Testcontainers
 */
@Disabled("Testcontainers incompatible with Docker 29+ (API v1.44 minimum)")
@SpringBootTest
@Testcontainers
@TestPropertySource(properties = {
        "spring.kafka.listener.auto-startup=false",
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration"
})
class ProductServiceApplicationTests {

    /** Testcontainers-managed PostgreSQL instance with auto-configured DataSource. */
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    /** Mock Elasticsearch repository to avoid requiring an ES cluster. */
    @MockBean
    private ProductSearchRepository productSearchRepository;

    /** Mock Elasticsearch operations bean. */
    @MockBean
    private ElasticsearchOperations elasticsearchOperations;

    /**
     * Smoke test: verifies the Spring application context starts without errors.
     */
    @Test
    void contextLoads() {
    }

}
