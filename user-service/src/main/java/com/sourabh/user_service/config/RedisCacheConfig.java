package com.sourabh.user_service.config;

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
 * Redis cache configuration for the user-service.
 *
 * <p>Enables Spring's {@code @Cacheable} / {@code @CacheEvict} abstraction
 * backed by a Redis store. Cached entries use JSON serialisation (via Jackson)
 * so that they are human-readable in Redis and portable across JVM restarts.</p>
 *
 * <h3>Cache regions</h3>
 * <ul>
 *   <li>{@value #USERS_BY_EMAIL_CACHE} &ndash; keyed by email address
 *       (used heavily by auth-service on every login)</li>
 *   <li>{@value #USERS_BY_UUID_CACHE} &ndash; keyed by user UUID</li>
 * </ul>
 *
 * <p>All caches default to a 15-minute TTL.</p>
 *
 * @see org.springframework.cache.annotation.Cacheable
 * @see org.springframework.cache.annotation.CacheEvict
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    /** Cache region name for user lookups by email. */
    public static final String USERS_BY_EMAIL_CACHE = "usersByEmail";

    /** Cache region name for user lookups by UUID. */
    public static final String USERS_BY_UUID_CACHE  = "usersByUuid";

    /**
     * Builds and configures the {@link RedisCacheManager} bean.
     *
     * <p>The manager is set up with:</p>
     * <ul>
     *   <li>String key serialisation</li>
     *   <li>Jackson-based JSON value serialisation (with Java-time support
     *       and default typing enabled for polymorphic caching)</li>
     *   <li>Null-value caching disabled</li>
     *   <li>15-minute TTL for all cache regions</li>
     * </ul>
     *
     * @param factory the {@link RedisConnectionFactory} auto-configured by
     *                Spring Boot
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
        cacheConfigs.put(USERS_BY_EMAIL_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigs.put(USERS_BY_UUID_CACHE,  defaultConfig.entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(15)))
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
