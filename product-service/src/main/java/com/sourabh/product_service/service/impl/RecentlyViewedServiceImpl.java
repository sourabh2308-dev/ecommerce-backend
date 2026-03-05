package com.sourabh.product_service.service.impl;

import com.sourabh.product_service.service.RecentlyViewedService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Redis-backed implementation of {@link RecentlyViewedService}.
 *
 * <p>Stores recently viewed product UUIDs per user in a Redis sorted set.
 * The score of each entry is the epoch-millisecond timestamp of the view,
 * allowing efficient retrieval of the most-recent items in descending order.
 * The sorted set is automatically trimmed to {@value #MAX_ITEMS} entries
 * to bound memory usage.</p>
 *
 * <p>Key format: {@code recently_viewed:{userUuid}}</p>
 *
 * @see RecentlyViewedService
 */
@Service
@RequiredArgsConstructor
public class RecentlyViewedServiceImpl implements RecentlyViewedService {

    /** Spring Data Redis template for string-based sorted-set operations. */
    private final StringRedisTemplate redisTemplate;

    /** Redis key prefix for the per-user recently-viewed sorted set. */
    private static final String KEY_PREFIX = "recently_viewed:";

    /** Maximum number of recently-viewed products retained per user. */
    private static final int MAX_ITEMS = 50;

    /**
     * {@inheritDoc}
     *
     * <p>Adds or updates the product UUID in the user's sorted set with
     * the current timestamp as score, then trims the set so only the
     * latest {@value #MAX_ITEMS} entries remain.</p>
     */
    @Override
    public void recordView(String userUuid, String productUuid) {
        String key = KEY_PREFIX + userUuid;
        redisTemplate.opsForZSet().add(key, productUuid, System.currentTimeMillis());
        redisTemplate.opsForZSet().removeRange(key, 0, -(MAX_ITEMS + 1));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Performs a reverse-range query (highest score first) to return
     * the most recently viewed product UUIDs.</p>
     */
    @Override
    public List<String> getRecentlyViewed(String userUuid, int limit) {
        String key = KEY_PREFIX + userUuid;
        Set<String> uuids = redisTemplate.opsForZSet()
                .reverseRange(key, 0, limit - 1);
        if (uuids == null) return Collections.emptyList();
        return List.copyOf(uuids);
    }
}
