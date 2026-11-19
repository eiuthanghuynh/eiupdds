package com.eiu.pdds.common.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Distributed atomic Redis rate limiter backed by an INCR/PEXPIRE/PTTL Lua script.
 * Fails open when Redis is unreachable or returns an unexpected response.
 */
@Slf4j
@Service
@Primary
@Profile("!test")
public class RedisRateLimiter implements RateLimiter {

    private static final String RATE_LIMIT_LUA = """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            local ttl = redis.call('PTTL', KEYS[1])
            if ttl < 0 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
                ttl = tonumber(ARGV[1])
            end
            return {current, ttl}
            """;

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> RATE_LIMIT_SCRIPT = createRateLimitScript();

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    @Autowired
    public RedisRateLimiter(StringRedisTemplate redisTemplate) {
        this(redisTemplate, Clock.systemUTC());
    }

    RedisRateLimiter(StringRedisTemplate redisTemplate, Clock clock) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @SuppressWarnings("rawtypes")
    private static DefaultRedisScript<List> createRateLimitScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(RATE_LIMIT_LUA);
        script.setResultType(List.class);
        return script;
    }

    @Override
    public RateLimitResult tryConsume(String key, int maxRequests, Duration window) {
        Objects.requireNonNull(key, "Rate limit key must not be null");
        if (key.isBlank()) {
            throw new IllegalArgumentException("Rate limit key must not be blank");
        }
        Objects.requireNonNull(window, "Rate limit window must not be null");

        int effectiveLimit = Math.max(1, maxRequests);
        long windowMillis = Math.max(1000L, window.toMillis());

        try {
            List<?> raw = redisTemplate.execute(
                    RATE_LIMIT_SCRIPT,
                    List.of(key),
                    String.valueOf(windowMillis)
            );
            if (raw == null || raw.size() < 2
                    || !(raw.get(0) instanceof Number currentNum)
                    || !(raw.get(1) instanceof Number ttlNum)) {
                log.warn("Rate limiter Redis returned unexpected result (fail-open) for key={}", key);
                return RateLimitResult.failOpen(effectiveLimit);
            }

            return RateLimitResult.ofCount(
                    currentNum.longValue(),
                    effectiveLimit,
                    ttlNum.longValue(),
                    clock.millis()
            );
        } catch (Exception e) {
            log.warn("Rate limiter Redis unavailable (fail-open) for key={}: {}", key, e.getMessage());
            return RateLimitResult.failOpen(effectiveLimit);
        }
    }
}
