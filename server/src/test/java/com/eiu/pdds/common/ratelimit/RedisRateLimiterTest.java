package com.eiu.pdds.common.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisRateLimiterTest {

    private static final Instant FIXED_INSTANT = Instant.ofEpochSecond(1_700_000_000L);
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Mock
    private StringRedisTemplate redisTemplate;

    private RedisRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RedisRateLimiter(redisTemplate, FIXED_CLOCK);
    }

    @Test
    @DisplayName("Should allow request under limit and compute remaining and resetEpochSeconds")
    @SuppressWarnings("unchecked")
    void tryConsume_underLimit_returnsAllowed() {
        String key = "pdds:rl:diag:guest:203.0.113.10";
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of(key)), eq("60000")))
                .thenReturn(List.of(1L, 59_500L));

        RateLimitResult result = rateLimiter.tryConsume(key, 3, Duration.ofSeconds(60));

        assertThat(result.allowed()).isTrue();
        assertThat(result.limit()).isEqualTo(3);
        assertThat(result.remaining()).isEqualTo(2);
        assertThat(result.retryAfterSeconds()).isZero();
        assertThat(result.resetEpochSeconds()).isEqualTo(1_700_000_060L);
    }

    @Test
    @DisplayName("Should allow request exactly at limit with zero remaining")
    @SuppressWarnings("unchecked")
    void tryConsume_atLimit_returnsAllowedWithZeroRemaining() {
        String key = "pdds:rl:diag:guest:203.0.113.10";
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of(key)), eq("60000")))
                .thenReturn(List.of(3L, 45_000L));

        RateLimitResult result = rateLimiter.tryConsume(key, 3, Duration.ofSeconds(60));

        assertThat(result.allowed()).isTrue();
        assertThat(result.limit()).isEqualTo(3);
        assertThat(result.remaining()).isZero();
        assertThat(result.retryAfterSeconds()).isZero();
        assertThat(result.resetEpochSeconds()).isEqualTo(1_700_000_045L);
    }

    @Test
    @DisplayName("Should block request over limit and round retryAfterSeconds up to whole seconds")
    @SuppressWarnings("unchecked")
    void tryConsume_overLimit_returnsBlockedWithRetryAfter() {
        String key = "pdds:rl:diag:guest:203.0.113.10";
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of(key)), eq("60000")))
                .thenReturn(List.of(4L, 44_200L));

        RateLimitResult result = rateLimiter.tryConsume(key, 3, Duration.ofSeconds(60));

        assertThat(result.allowed()).isFalse();
        assertThat(result.limit()).isEqualTo(3);
        assertThat(result.remaining()).isZero();
        assertThat(result.retryAfterSeconds()).isEqualTo(45L);
        assertThat(result.resetEpochSeconds()).isEqualTo(1_700_000_045L);
    }

    @Test
    @DisplayName("Should fail-open when Redis raises RedisConnectionFailureException")
    @SuppressWarnings("unchecked")
    void tryConsume_redisConnectionFailure_failsOpen() {
        String key = "pdds:rl:auth:ip:198.51.100.5";
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of(key)), eq("60000")))
                .thenThrow(new RedisConnectionFailureException("Redis connection refused"));

        RateLimitResult result = rateLimiter.tryConsume(key, 3, Duration.ofSeconds(60));

        assertThat(result.allowed()).isTrue();
        assertThat(result.limit()).isEqualTo(3);
        assertThat(result.remaining()).isEqualTo(3);
        assertThat(result.retryAfterSeconds()).isZero();
        assertThat(result.resetEpochSeconds()).isPositive();
    }

    @Test
    @DisplayName("Should fail-open when Lua script returns null or malformed list")
    @SuppressWarnings("unchecked")
    void tryConsume_nullOrIncompleteResult_failsOpen() {
        String key = "pdds:rl:auth:ip:198.51.100.6";
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of(key)), eq("60000")))
                .thenReturn(null)
                .thenReturn(List.of(1L))
                .thenReturn(List.of("invalid", "ttl"));

        for (int i = 0; i < 3; i++) {
            RateLimitResult result = rateLimiter.tryConsume(key, 3, Duration.ofSeconds(60));
            assertThat(result.allowed()).isTrue();
            assertThat(result.limit()).isEqualTo(3);
            assertThat(result.remaining()).isEqualTo(3);
            assertThat(result.retryAfterSeconds()).isZero();
        }
    }

    @Test
    @DisplayName("Should pass atomic INCR/PEXPIRE/PTTL Lua script, key list, and window millis to Redis")
    @SuppressWarnings("unchecked")
    void tryConsume_passesExpectedLuaScriptAndArguments() {
        String key = "pdds:rl:diag:user:42";
        when(redisTemplate.execute(any(RedisScript.class), eq(List.of(key)), anyString()))
                .thenReturn(List.of(1L, 1_000L));

        rateLimiter.tryConsume(key, 10, Duration.ofMillis(250));

        ArgumentCaptor<RedisScript<List>> scriptCaptor = ArgumentCaptor.forClass(RedisScript.class);
        verify(redisTemplate).execute(scriptCaptor.capture(), eq(List.of(key)), eq("1000"));

        RedisScript<List> capturedScript = scriptCaptor.getValue();
        assertThat(capturedScript.getResultType()).isEqualTo(List.class);
        assertThat(capturedScript.getScriptAsString())
                .contains("redis.call('INCR', KEYS[1])")
                .contains("redis.call('PEXPIRE', KEYS[1], ARGV[1])")
                .contains("redis.call('PTTL', KEYS[1])")
                .contains("return {current, ttl}");
    }

    @Test
    @DisplayName("Should reject null or blank inputs before calling Redis")
    void tryConsume_invalidInputs_throwsException() {
        assertThatThrownBy(() -> rateLimiter.tryConsume(null, 3, Duration.ofSeconds(60)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> rateLimiter.tryConsume("   ", 3, Duration.ofSeconds(60)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rateLimiter.tryConsume("valid-key", 3, null))
                .isInstanceOf(NullPointerException.class);
    }
}
