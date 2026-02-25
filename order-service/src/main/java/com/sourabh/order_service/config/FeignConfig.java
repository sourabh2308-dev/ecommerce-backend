package com.sourabh.order_service.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
public class FeignConfig {

    @Value("${internal.secret}")
    // Dependency injected by Spring container
    // @Value - Automatic dependency injection at runtime
    // Dependency injected by Spring container
    // @Value - Automatic dependency injection at runtime
    private String internalSecret;

    /**
     * Adds X-Internal-Secret to every outbound Feign request so
     * downstream services' InternalSecretFilter accepts the call.
     * Named 'feignOutboundSecretInterceptor' to avoid collision with the
     * inbound InternalSecretInterceptor security @Component.
     */
    @Bean
    /**
     * FEIGNOUTBOUNDSECRETINTERCEPTOR - Method Documentation
     *
     * PURPOSE:
     * This method handles the feignOutboundSecretInterceptor operation.
     *
     * RETURN VALUE:
     * @return RequestInterceptor - Result of the operation
     *
     * ANNOTATIONS USED:
     * @Value - Applied to this method
     * @Component - Applied to this method
     * @Bean - Applied to this method
     *
     */
    public RequestInterceptor feignOutboundSecretInterceptor() {
        return template -> template.header("X-Internal-Secret", internalSecret);
    }
}
