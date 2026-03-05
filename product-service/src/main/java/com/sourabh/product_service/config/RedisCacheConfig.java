package com.sourabh.product_service.config;

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
 * Redis-backed caching configuration for the product-service.
 *
 * <p>Enables Spring's cache abstraction ({@code @Cacheable}, {@code @CacheEvict},
 * etc.) and defines how cached values are serialised and how long they survive
 * in Redis.
 *
 * <h3>Serialisation</h3>
 * <ul>
 *   <li><b>Keys</b> are serialised as plain UTF-8 strings.</li>
 *   <li><b>Values</b> are serialised as JSON using Jackson with
 *       {@code JavaTimeModule} (so that {@code LocalDateTime} fields are
 *       stored in ISO-8601 format) and default typing enabled for
 *       polymorphic deserialisation.</li>
 * </ul>
 *
 * <h3>TTL Policy</h3>
 * The default and per-cache TTL is currently set to <strong>10 minutes</strong>.
 *
 * @see org.springframework.cache.annotation.Cacheable
 * @see org.springframework.cache.annotation.CacheEvict
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     * Logical cache name used in {@code @Cacheable} / {@code @CacheEvict}
     * annotations throughout the product-service.
     */
    public static final String PRODUCTS_CACHE = "products";

    /**
     * Creates a {@link RedisCacheManager} with JSON value serialisation and
     * per-cache TTL overrides.
     *
     * <p>The {@link ObjectMapper} is configured with:
     * <ul>
     *   <li>{@code JavaTimeModule} for Java 8 date/time support.</li>
     *   <li>Timestamps disabled — dates are written as ISO-8601 strings.</li>
     *   <li>Default typing enabled so that polymorphic types can be
     *       round-tripped through the cache.</li>
     * </ul>
     *
     * @param factory the {@link RedisConnectionFactory} auto-configured by
     *                Spring Boot from {@code application.yml} properties
     * @return a fully-configured {@code RedisCacheManager}
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
        cacheConfigs.put(PRODUCTS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
