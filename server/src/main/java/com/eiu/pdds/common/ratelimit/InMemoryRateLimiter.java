package com.eiu.pdds.common.ratelimit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory rate limiter for the {@code test} profile without a live Redis server.
 */
@Service
@Profile("test")
public class InMemoryRateLimiter implements RateLimiter {

    private final ConcurrentHashMap<String, WindowState> buckets = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public InMemoryRateLimiter() {
        this(Clock.systemUTC());
    }

    InMemoryRateLimiter(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
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

        WindowState state = buckets.computeIfAbsent(key, k -> new WindowState(clock.millis()));
        synchronized (state) {
            long nowMillis = clock.millis();
            if (nowMillis - state.windowStartMillis >= windowMillis) {
                state.windowStartMillis = nowMillis;
                state.count = 0L;
            }
            state.count++;
            long ttlMillis = Math.max(1L, (state.windowStartMillis + windowMillis) - nowMillis);
            return RateLimitResult.ofCount(state.count, effectiveLimit, ttlMillis, nowMillis);
        }
    }

    /**
     * Clears all active rate-limit buckets, intended for test isolation between cases.
     */
    public void clear() {
        buckets.clear();
    }

    private static final class WindowState {
        private long windowStartMillis;
        private long count;

        private WindowState(long windowStartMillis) {
            this.windowStartMillis = windowStartMillis;
            this.count = 0L;
        }
    }
}
