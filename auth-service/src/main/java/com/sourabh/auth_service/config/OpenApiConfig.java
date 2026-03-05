package com.sourabh.auth_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger configuration for the auth-service.
 *
 * <p>Registers a custom {@link OpenAPI} bean that sets the server URL to
 * {@code "/"}, ensuring the generated API documentation uses relative paths
 * compatible with reverse-proxy / API Gateway routing.</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Creates a customised {@link OpenAPI} descriptor with a root-relative
     * server entry.
     *
     * @return the configured {@link OpenAPI} instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        Server server = new Server();
        server.setUrl("/");
        return new OpenAPI().servers(List.of(server));
    }
}
