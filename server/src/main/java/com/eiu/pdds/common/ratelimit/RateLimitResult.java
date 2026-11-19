package com.eiu.pdds.common.ratelimit;

import java.time.Instant;

/**
 * Immutable outcome of a single rate-limit evaluation.
 */
public record RateLimitResult(
        boolean allowed,
        int limit,
        int remaining,
        long resetEpochSeconds,
        long retryAfterSeconds
) {
    /**
     * Creates a permissive fail-open result used when the backing store is unavailable.
     */
    public static RateLimitResult failOpen(int limit) {
        return new RateLimitResult(true, limit, limit, Instant.now().getEpochSecond(), 0L);
    }

    static RateLimitResult ofCount(long currentCount, int maxRequests, long ttlMillis, long nowMillis) {
        long safeTtlMillis = Math.max(1L, ttlMillis);
        boolean allowed = currentCount <= maxRequests;
        int remaining = (int) Math.max(0L, (long) maxRequests - currentCount);
        long resetEpochSeconds = (nowMillis + safeTtlMillis + 999L) / 1000L;
        long retryAfterSeconds = allowed ? 0L : Math.max(1L, (safeTtlMillis + 999L) / 1000L);
        return new RateLimitResult(allowed, maxRequests, remaining, resetEpochSeconds, retryAfterSeconds);
    }
}
