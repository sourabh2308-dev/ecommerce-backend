package com.sourabh.user_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI / Swagger configuration for the user-service.
 *
 * <p>Customises the auto-generated OpenAPI specification so that the
 * Swagger UI "Try it out" feature targets the correct base URL, which is
 * especially important when the service runs behind the API Gateway or
 * inside a containerised environment where the public URL differs from
 * the service's internal address.</p>
 *
 * @see <a href="https://springdoc.org/">springdoc-openapi</a>
 */
@Configuration
public class OpenApiConfig {

    /**
     * Provides a customised {@link OpenAPI} bean with a root-relative server URL.
     *
     * <p>Setting the server URL to {@code "/"} ensures that Swagger UI sends
     * requests to the same host from which the UI was loaded, avoiding
     * cross-origin issues in local and containerised setups.</p>
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