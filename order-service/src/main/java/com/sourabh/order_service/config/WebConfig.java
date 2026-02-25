package com.sourabh.order_service.config;

import com.sourabh.order_service.security.InternalSecretInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Spring Configuration - Defines beans and infrastructure setup
@Configuration
@RequiredArgsConstructor
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
public class WebConfig implements WebMvcConfigurer {

    private final InternalSecretInterceptor internalSecretInterceptor;

    @Override
    /**
     * ADDINTERCEPTORS - Method Documentation
     *
     * PURPOSE:
     * This method handles the addInterceptors operation.
     *
     * PARAMETERS:
     * @param registry - InterceptorRegistry value
     *
     * ANNOTATIONS USED:
     * @Autowired - Applied to this method
     * @Override - Implements interface method
     *
     */
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(internalSecretInterceptor)
                .addPathPatterns("/api/orders/**");
    }
}
