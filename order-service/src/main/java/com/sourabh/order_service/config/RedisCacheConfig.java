package com.sourabh.order_service.config;

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

/**
 * Spring configuration class that sets up Redis-backed caching for the
 * order-service.
 *
 * <p>Enables Spring's annotation-driven caching abstraction via
 * {@link EnableCaching} and provides a custom {@link RedisCacheManager}
 * bean configured with:</p>
 * <ul>
 *   <li>JSON value serialisation using Jackson (with Java 8 date/time support)</li>
 *   <li>String key serialisation</li>
 *   <li>Null-value caching disabled</li>
 *   <li>Per-cache TTL configuration (default: 10 minutes)</li>
 * </ul>
 *
 * @see org.springframework.cache.annotation.Cacheable
 * @see org.springframework.cache.annotation.CacheEvict
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * Logical cache name used throughout the order-service in
     * {@code @Cacheable} and {@code @CacheEvict} annotations.
     */
    public static final String ORDERS_CACHE = "orders";

    /**
     * Creates and configures a {@link RedisCacheManager} with custom JSON
     * serialisation and a 10-minute default TTL.
     *
     * <p>An {@link ObjectMapper} is configured with the {@link JavaTimeModule}
     * for correct serialisation of {@code java.time.*} types and default
     * typing enabled to preserve polymorphic type information in cached
     * values.</p>
     *
     * @param factory the {@link RedisConnectionFactory} auto-configured by Spring Boot
     * @return a fully configured {@link RedisCacheManager}
     */
    @Bean
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
        cacheConfigs.put(ORDERS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
