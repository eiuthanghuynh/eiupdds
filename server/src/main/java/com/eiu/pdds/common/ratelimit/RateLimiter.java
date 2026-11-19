package com.eiu.pdds.common.ratelimit;

import java.time.Duration;

/**
 * Abstraction for consuming quota tokens against a rate-limit bucket.
 */
public interface RateLimiter {
    RateLimitResult tryConsume(String key, int maxRequests, Duration window);
}
