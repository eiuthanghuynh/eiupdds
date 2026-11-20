package com.eiu.pdds.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed user auth state cache with strict fail-open behavior on Redis or serialization errors.
 */
@Slf4j
@Service
@Primary
@Profile("!test")
public class RedisUserAuthCache implements UserAuthCache {

    private static final String KEY_PREFIX = "pdds:auth:user-state:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    @Autowired
    public RedisUserAuthCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${pdds.auth.user-cache-ttl-seconds:180}") long ttlSeconds
    ) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.ttl = Duration.ofSeconds(Math.max(1L, ttlSeconds));
    }

    @Override
    public Optional<CachedUserAuthState> get(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }

        String key = buildKey(userId);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            CachedUserAuthState state = objectMapper.readValue(json, CachedUserAuthState.class);
            if (state == null || state.id() == null || (state.isUsable() && state.role() == null)) {
                log.warn("Invalid cached user auth state payload in Redis (fail-open) for userId={}", userId);
                return Optional.empty();
            }
            return Optional.of(state);
        } catch (Exception e) {
            log.warn("Redis user auth cache get failed (fail-open) for userId={}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(CachedUserAuthState state) {
        if (state == null || state.id() == null) {
            return;
        }

        String key = buildKey(state.id());
        try {
            String json = objectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            log.warn("Redis user auth cache put failed (fail-open) for userId={}: {}", state.id(), e.getMessage());
        }
    }

    @Override
    public void evict(UUID userId) {
        if (userId == null) {
            return;
        }

        String key = buildKey(userId);
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis user auth cache evict failed (fail-open) for userId={}: {}", userId, e.getMessage());
        }
    }

    private String buildKey(UUID userId) {
        return KEY_PREFIX + userId;
    }
}
