package com.sourabh.payment_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) configuration for the payment service.
 *
 * <p>Registers a custom {@link OpenAPI} bean that sets the server base URL
 * to {@code "/"}.  This ensures the Swagger UI and generated client stubs
 * resolve endpoint paths relative to the host rather than using an absolute
 * URL that may not match the actual deployment behind the API Gateway.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates a customised {@link OpenAPI} descriptor with a single
     * relative-root server entry.
     *
     * @return the configured OpenAPI instance used by SpringDoc
     */
    @Bean
    public OpenAPI customOpenAPI() {
        Server server = new Server();
        server.setUrl("/");
        return new OpenAPI().servers(List.of(server));
    }
}
