package com.eiu.pdds.auth.security;

import com.eiu.pdds.user.entity.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisUserAuthCacheTest {

    private static final long TTL_SECONDS = 180L;
    private static final Duration EXPECTED_TTL = Duration.ofSeconds(TTL_SECONDS);

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private RedisUserAuthCache cache;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        cache = new RedisUserAuthCache(redisTemplate, objectMapper, TTL_SECONDS);
    }

    @Test
    @DisplayName("put and get round-trip CachedUserAuthState using exact key prefix and configured TTL")
    void putAndGet_validState_storesJsonAndReturnsCachedState() {
        UUID userId = UUID.randomUUID();
        String expectedKey = "pdds:auth:user-state:" + userId;
        CachedUserAuthState state = new CachedUserAuthState(
                userId,
                "farmer@eiu.edu.vn",
                "farmer_one",
                Role.ROLE_USER,
                true,
                false
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        cache.put(state);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(expectedKey), jsonCaptor.capture(), eq(EXPECTED_TTL));
        assertThat(jsonCaptor.getValue()).doesNotContain("\"usable\"");

        when(valueOperations.get(expectedKey)).thenReturn(jsonCaptor.getValue());

        Optional<CachedUserAuthState> result = cache.get(userId);
        assertThat(result).contains(state);
        assertThat(result.get().isUsable()).isTrue();
    }

    @Test
    @DisplayName("get returns Optional.empty() on cache miss or blank payload")
    void get_cacheMissOrBlank_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        String expectedKey = "pdds:auth:user-state:" + userId;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(null).thenReturn("   ");

        assertThat(cache.get(userId)).isEmpty();
        assertThat(cache.get(userId)).isEmpty();
    }

    @Test
    @DisplayName("evict deletes the exact user auth state key")
    void evict_deletesUserKey() {
        UUID userId = UUID.randomUUID();
        String expectedKey = "pdds:auth:user-state:" + userId;

        cache.evict(userId);

        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    @DisplayName("constructor clamps non-positive TTL seconds to minimum 1 second")
    void constructor_nonPositiveTtl_clampsToMinimumOneSecond() {
        RedisUserAuthCache shortCache = new RedisUserAuthCache(redisTemplate, objectMapper, 0L);
        UUID userId = UUID.randomUUID();
        CachedUserAuthState state = new CachedUserAuthState(
                userId, "a@eiu.edu.vn", "user_a", Role.ROLE_ADMIN, true, false
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        shortCache.put(state);

        verify(valueOperations).set(eq("pdds:auth:user-state:" + userId), anyString(), eq(Duration.ofSeconds(1)));
    }

    @Test
    @DisplayName("null userId or invalid state safely no-ops without touching Redis")
    void nullGuards_neverInteractWithRedis() {
        assertThat(cache.get(null)).isEmpty();
        cache.put(null);
        cache.put(new CachedUserAuthState(null, "x@eiu.edu.vn", "x", Role.ROLE_USER, true, false));
        cache.evict(null);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("Redis outage fails open across get, put, and evict without throwing")
    void redisOutage_failsOpenAcrossAllOperations() {
        UUID userId = UUID.randomUUID();
        String expectedKey = "pdds:auth:user-state:" + userId;
        CachedUserAuthState state = new CachedUserAuthState(
                userId, "outage@eiu.edu.vn", "outage_user", Role.ROLE_EXPERT, true, false
        );

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenThrow(new RedisConnectionFailureException("Redis down"));
        doThrow(new RedisConnectionFailureException("Redis down"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        when(redisTemplate.delete(expectedKey)).thenThrow(new RedisConnectionFailureException("Redis down"));

        assertThat(cache.get(userId)).isEmpty();
        assertThatCode(() -> cache.put(state)).doesNotThrowAnyException();
        assertThatCode(() -> cache.evict(userId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("corrupt or structurally incomplete JSON payload in Redis fails open returning Optional.empty()")
    void get_corruptOrIncompleteJson_failsOpenReturnsEmpty() {
        UUID userId = UUID.randomUUID();
        String expectedKey = "pdds:auth:user-state:" + userId;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey))
                .thenReturn("{not-valid-json")
                .thenReturn("{}")
                .thenReturn("{\"id\":\"" + userId + "\",\"active\":true,\"deleted\":false}");

        assertThat(cache.get(userId)).isEmpty();
        assertThat(cache.get(userId)).isEmpty();
        assertThat(cache.get(userId)).isEmpty();
    }
}
