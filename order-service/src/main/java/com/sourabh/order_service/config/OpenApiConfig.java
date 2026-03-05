package com.sourabh.order_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Spring configuration class that customises the OpenAPI (Swagger) specification
 * generated for the order-service.
 *
 * <p>Primarily sets the server URL to {@code "/"} so that the Swagger UI
 * constructs request URLs relative to the current host, which works
 * correctly both when accessed directly and when routed through the API
 * Gateway.</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates a custom {@link OpenAPI} bean with a root-relative server entry.
     *
     * @return the configured {@link OpenAPI} instance used by springdoc
     */
    @Bean
    public OpenAPI customOpenAPI() {
        Server server = new Server();
        server.setUrl("/");
        return new OpenAPI().servers(List.of(server));
    }
}