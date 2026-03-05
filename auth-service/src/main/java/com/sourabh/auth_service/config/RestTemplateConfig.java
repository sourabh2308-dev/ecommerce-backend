package com.sourabh.auth_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class that provides a {@link RestTemplate} bean for
 * synchronous REST calls to other microservices (primarily
 * {@code user-service}).
 *
 * <p>The default {@code RestTemplate} is sufficient for this service;
 * custom timeouts or interceptors can be added here if required.</p>
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Creates a default {@link RestTemplate} managed by the Spring container.
     *
     * @return a new {@link RestTemplate} instance
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
