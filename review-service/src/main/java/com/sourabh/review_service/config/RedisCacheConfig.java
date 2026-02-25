package com.sourabh.review_service.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

// Spring Configuration - Defines beans and infrastructure setup
@Configuration
@EnableCaching
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
public class RedisCacheConfig {

    /** Cache name — used in @Cacheable/@CacheEvict throughout review-service */
    public static final String REVIEWS_CACHE = "reviews";

    @Bean
    /**
     * CACHEMANAGER - Method Documentation
     *
     * PURPOSE:
     * This method handles the cacheManager operation.
     *
     * PARAMETERS:
     * @param factory - RedisConnectionFactory value
     *
     * RETURN VALUE:
     * @return RedisCacheManager - Result of the operation
     *
     * ANNOTATIONS USED:
     * @Autowired - Applied to this method
     * @Cacheable - Caches result for performance
     * @Bean - Applied to this method
     *
     */
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {

        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL);
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(om);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration
                .defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(REVIEWS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
