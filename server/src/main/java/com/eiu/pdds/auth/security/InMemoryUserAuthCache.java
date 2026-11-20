package com.eiu.pdds.auth.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory user auth state cache for test profile execution with TTL and Clock injection.
 */
@Service
@Profile("test")
public class InMemoryUserAuthCache implements UserAuthCache {

    private final ConcurrentHashMap<UUID, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;

    @Autowired
    public InMemoryUserAuthCache(@Value("${pdds.auth.user-cache-ttl-seconds:180}") long ttlSeconds) {
        this(ttlSeconds, Clock.systemUTC());
    }

    public InMemoryUserAuthCache(long ttlSeconds, Clock clock) {
        this.ttl = Duration.ofSeconds(Math.max(1L, ttlSeconds));
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Optional<CachedUserAuthState> get(UUID userId) {
        if (userId == null) {
            return Optional.empty();
        }
        CacheEntry entry = cache.get(userId);
        if (entry == null) {
            return Optional.empty();
        }
        if (!clock.instant().isBefore(entry.expiresAt())) {
            cache.remove(userId, entry);
            return Optional.empty();
        }
        return Optional.of(entry.state());
    }

    @Override
    public void put(CachedUserAuthState state) {
        if (state == null || state.id() == null) {
            return;
        }
        cache.put(state.id(), new CacheEntry(state, clock.instant().plus(ttl)));
    }

    @Override
    public void evict(UUID userId) {
        if (userId == null) {
            return;
        }
        cache.remove(userId);
    }

    public void clear() {
        cache.clear();
    }

    private record CacheEntry(CachedUserAuthState state, Instant expiresAt) {
    }
}
