package com.sourabh.product_service.service.impl;

import com.sourabh.product_service.service.RecentlyViewedService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Stores recently viewed products per user in a Redis sorted set.
 * Score = timestamp, so we can retrieve most-recent first.
 * Max 50 items per user; older entries auto-trimmed.
 */
@Service
@RequiredArgsConstructor
public class RecentlyViewedServiceImpl implements RecentlyViewedService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "recently_viewed:";
    private static final int MAX_ITEMS = 50;

    @Override
    public void recordView(String userUuid, String productUuid) {
        String key = KEY_PREFIX + userUuid;
        redisTemplate.opsForZSet().add(key, productUuid, System.currentTimeMillis());
        // Trim to keep only the latest MAX_ITEMS
        redisTemplate.opsForZSet().removeRange(key, 0, -(MAX_ITEMS + 1));
    }

    @Override
    public List<String> getRecentlyViewed(String userUuid, int limit) {
        String key = KEY_PREFIX + userUuid;
        Set<String> uuids = redisTemplate.opsForZSet()
                .reverseRange(key, 0, limit - 1);
        if (uuids == null) return Collections.emptyList();
        return List.copyOf(uuids);
    }
}
