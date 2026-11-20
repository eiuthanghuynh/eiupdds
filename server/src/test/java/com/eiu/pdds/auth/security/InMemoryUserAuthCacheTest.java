package com.eiu.pdds.auth.security;

import com.eiu.pdds.user.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryUserAuthCacheTest {

    private MutableClock clock;
    private InMemoryUserAuthCache cache;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.ofEpochSecond(1_700_000_000L));
        cache = new InMemoryUserAuthCache(180L, clock);
    }

    @Test
    @DisplayName("put and get returns cached state before TTL expires and expires deterministically at TTL")
    void putAndGet_expiresAtConfiguredTtl() {
        UUID userId = UUID.randomUUID();
        CachedUserAuthState state = new CachedUserAuthState(
                userId, "test@eiu.edu.vn", "test_user", Role.ROLE_USER, true, false
        );

        cache.put(state);

        clock.advance(Duration.ofSeconds(179));
        assertThat(cache.get(userId)).contains(state);

        clock.advance(Duration.ofSeconds(1));
        assertThat(cache.get(userId)).isEmpty();
    }

    @Test
    @DisplayName("evict and clear remove cached entries immediately")
    void evictAndClear_removeEntriesImmediately() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        cache.put(new CachedUserAuthState(userA, "a@eiu.edu.vn", "user_a", Role.ROLE_USER, true, false));
        cache.put(new CachedUserAuthState(userB, "b@eiu.edu.vn", "user_b", Role.ROLE_ADMIN, true, false));

        cache.evict(userA);
        assertThat(cache.get(userA)).isEmpty();
        assertThat(cache.get(userB)).isPresent();

        cache.clear();
        assertThat(cache.get(userB)).isEmpty();
    }

    @Test
    @DisplayName("null inputs safely no-op")
    void nullInputs_safelyNoOp() {
        assertThat(cache.get(null)).isEmpty();
        cache.put(null);
        cache.put(new CachedUserAuthState(null, "x@eiu.edu.vn", "x", Role.ROLE_USER, true, false));
        cache.evict(null);
    }

    private static final class MutableClock extends Clock {
        private Instant currentInstant;

        private MutableClock(Instant initialInstant) {
            this.currentInstant = initialInstant;
        }

        private void advance(Duration duration) {
            this.currentInstant = this.currentInstant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }
}
