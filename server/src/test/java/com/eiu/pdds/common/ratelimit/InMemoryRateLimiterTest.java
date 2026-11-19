package com.eiu.pdds.common.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryRateLimiterTest {

    private MutableClock clock;
    private InMemoryRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.ofEpochSecond(1_700_000_000L));
        rateLimiter = new InMemoryRateLimiter(clock);
    }

    @Test
    @DisplayName("Should enforce exact quota and block excess requests with accurate retryAfterSeconds")
    void tryConsume_enforcesExactQuotaAndBlocksExcess() {
        String key = "pdds:rl:diag:guest:192.0.2.1";
        Duration window = Duration.ofSeconds(60);

        RateLimitResult first = rateLimiter.tryConsume(key, 3, window);
        RateLimitResult second = rateLimiter.tryConsume(key, 3, window);
        RateLimitResult third = rateLimiter.tryConsume(key, 3, window);

        assertThat(first.allowed()).isTrue();
        assertThat(first.limit()).isEqualTo(3);
        assertThat(first.remaining()).isEqualTo(2);
        assertThat(first.retryAfterSeconds()).isZero();
        assertThat(first.resetEpochSeconds()).isEqualTo(1_700_000_060L);

        assertThat(second.allowed()).isTrue();
        assertThat(second.remaining()).isEqualTo(1);
        assertThat(second.retryAfterSeconds()).isZero();

        assertThat(third.allowed()).isTrue();
        assertThat(third.remaining()).isZero();
        assertThat(third.retryAfterSeconds()).isZero();

        clock.advance(Duration.ofMillis(15_800));
        RateLimitResult blocked = rateLimiter.tryConsume(key, 3, window);

        assertThat(blocked.allowed()).isFalse();
        assertThat(blocked.limit()).isEqualTo(3);
        assertThat(blocked.remaining()).isZero();
        assertThat(blocked.retryAfterSeconds()).isEqualTo(45L);
        assertThat(blocked.resetEpochSeconds()).isEqualTo(1_700_000_060L);
    }

    @Test
    @DisplayName("Should isolate rate limit buckets across distinct keys")
    void tryConsume_separatesBucketsAcrossDistinctKeys() {
        Duration window = Duration.ofSeconds(60);

        assertThat(rateLimiter.tryConsume("key-a", 2, window).allowed()).isTrue();
        assertThat(rateLimiter.tryConsume("key-a", 2, window).allowed()).isTrue();
        assertThat(rateLimiter.tryConsume("key-a", 2, window).allowed()).isFalse();

        RateLimitResult otherKey = rateLimiter.tryConsume("key-b", 2, window);
        assertThat(otherKey.allowed()).isTrue();
        assertThat(otherKey.remaining()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reset quota once window elapses")
    void tryConsume_resetsAfterWindowElapses() {
        String key = "pdds:rl:auth:ip:198.51.100.77";
        Duration window = Duration.ofSeconds(60);

        assertThat(rateLimiter.tryConsume(key, 2, window).allowed()).isTrue();
        assertThat(rateLimiter.tryConsume(key, 2, window).allowed()).isTrue();

        clock.advance(Duration.ofSeconds(59));
        RateLimitResult stillBlocked = rateLimiter.tryConsume(key, 2, window);
        assertThat(stillBlocked.allowed()).isFalse();
        assertThat(stillBlocked.retryAfterSeconds()).isEqualTo(1L);

        clock.advance(Duration.ofSeconds(1));
        RateLimitResult afterReset = rateLimiter.tryConsume(key, 2, window);
        assertThat(afterReset.allowed()).isTrue();
        assertThat(afterReset.remaining()).isEqualTo(1);
        assertThat(afterReset.retryAfterSeconds()).isZero();
        assertThat(afterReset.resetEpochSeconds()).isEqualTo(1_700_000_120L);
    }

    @Test
    @DisplayName("Should reset active buckets immediately when clear() is called")
    void clear_resetsActiveBucketsImmediately() {
        String key = "pdds:rl:diag:user:user-1";
        Duration window = Duration.ofSeconds(60);

        assertThat(rateLimiter.tryConsume(key, 1, window).allowed()).isTrue();
        assertThat(rateLimiter.tryConsume(key, 1, window).allowed()).isFalse();

        rateLimiter.clear();

        RateLimitResult afterClear = rateLimiter.tryConsume(key, 1, window);
        assertThat(afterClear.allowed()).isTrue();
        assertThat(afterClear.remaining()).isZero();
    }

    @Test
    @DisplayName("Should enforce exact quota under 30 concurrent threads on the same key")
    void tryConsume_concurrentBurst_enforcesExactQuota() throws Exception {
        String key = "pdds:rl:burst:shared";
        Duration window = Duration.ofSeconds(60);
        int totalThreads = 30;
        int limit = 10;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch ready = new CountDownLatch(totalThreads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<RateLimitResult>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < totalThreads; i++) {
                Callable<RateLimitResult> task = () -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    return rateLimiter.tryConsume(key, limit, window);
                };
                futures.add(executor.submit(task));
            }

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            int allowedCount = 0;
            int blockedCount = 0;
            for (Future<RateLimitResult> future : futures) {
                RateLimitResult result = future.get(5, TimeUnit.SECONDS);
                if (result.allowed()) {
                    allowedCount++;
                } else {
                    blockedCount++;
                }
            }

            assertThat(allowedCount).isEqualTo(10);
            assertThat(blockedCount).isEqualTo(20);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Should reject null or blank key and null window")
    void tryConsume_invalidInputs_throwsException() {
        assertThatThrownBy(() -> rateLimiter.tryConsume(null, 3, Duration.ofSeconds(60)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> rateLimiter.tryConsume("", 3, Duration.ofSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rateLimiter.tryConsume("valid-key", 3, null))
                .isInstanceOf(NullPointerException.class);
    }

    private static final class MutableClock extends Clock {
        private final AtomicLong millis;

        private MutableClock(Instant initialInstant) {
            this.millis = new AtomicLong(initialInstant.toEpochMilli());
        }

        private void advance(Duration duration) {
            millis.addAndGet(duration.toMillis());
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
            return Instant.ofEpochMilli(millis.get());
        }

        @Override
        public long millis() {
            return millis.get();
        }
    }
}
