package com.sourabh.product_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) configuration for the product-service.
 *
 * <p>Registers a custom {@link OpenAPI} bean that sets the server URL to
 * {@code "/"}, ensuring that the Swagger UI generates correct request paths
 * regardless of whether the service is accessed directly or through the
 * API Gateway.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates a customised {@link OpenAPI} descriptor with a root-relative
     * server entry.
     *
     * @return the configured {@code OpenAPI} instance used by springdoc
     */
    @Bean
    public OpenAPI customOpenAPI() {
        Server server = new Server();
        server.setUrl("/");
        return new OpenAPI().servers(List.of(server));
    }
}