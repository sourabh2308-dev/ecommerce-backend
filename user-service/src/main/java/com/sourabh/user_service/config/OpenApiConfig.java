package com.sourabh.user_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

// Spring Configuration - Defines beans and infrastructure setup
@Configuration
/**
 * SPRING CONFIGURATION - Bean Definitions and App Setup
 * 
 * PURPOSE:
 * Defines Spring beans and application-level configuration.
 * Beans are singleton objects managed by Spring IoC container.
 * 
 * COMMON CONFIGURATION TYPES:
 * 
 * 1. FeignConfig:
 *    - Configures Feign client behavior (timeouts, error decoder)
 *    - Sets up request interceptors for adding headers
 * 
 * 2. KafkaConfig:
 *    - Producer: Serialization, acks, retries
 *    - Consumer: Deserialization, group ID, auto-commit
 * 
 * 3. CacheConfig:
 *    - Redis connection settings
 *    - Cache TTL (time-to-live) for @Cacheable
 * 
 * 4. WebConfig:
 *    - CORS mappings
 *    - Message converters (JSON, XML)
 *    - Interceptors
 * 
 * 5. AsyncConfig:
 *    - Thread pool for @Async methods
 *    - Executor configuration
 * 
 * BEAN LIFECYCLE:
 * @Bean annotations tell Spring to:
 * 1. Create instance of return type
 * 2. Manage as singleton in application context
 * 3. Inject into other classes via @Autowired
 * 
 * EXAMPLE:
 * @Bean
 * public RestTemplate restTemplate() {
 *   return new RestTemplate(); // Spring manages this instance
 * }
 * 
 * Usage in other classes:
 * @Autowired
 * private RestTemplate restTemplate; // Spring injects the bean
 */
public class OpenApiConfig {

    @Bean
    /**
     * CUSTOMOPENAPI - Method Documentation
     *
     * PURPOSE:
     * This method handles the customOpenAPI operation.
     *
     * RETURN VALUE:
     * @return OpenAPI - Result of the operation
     *
     * ANNOTATIONS USED:
     * @Autowired - Applied to this method
     * @Bean - Applied to this method
     *
     */
    public OpenAPI customOpenAPI() {
        Server server = new Server();
        server.setUrl("/");
        return new OpenAPI().servers(List.of(server));
    }
}