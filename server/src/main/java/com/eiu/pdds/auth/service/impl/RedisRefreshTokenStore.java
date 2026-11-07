package com.eiu.pdds.auth.service.impl;

import com.eiu.pdds.auth.model.RefreshTokenSession;
import com.eiu.pdds.auth.service.RefreshTokenStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Enterprise Production RefreshTokenStore backed by Redis.
 * Supports sliding window TTL, atomic token rotation (GETDEL), and Token Reuse Detection via tombstoning.
 */
@Slf4j
@Service
@Primary
@Profile("!test")
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "pdds:auth:token:";
    private static final String USER_INDEX_PREFIX = "pdds:auth:user:";
    private static final String CONSUMED_PREFIX = "pdds:auth:consumed:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void store(String tokenId, UUID userId, String deviceInfo, Duration ttl) {
        long now = System.currentTimeMillis();
        long expiresAt = now + ttl.toMillis();
        RefreshTokenSession session = new RefreshTokenSession(tokenId, userId, deviceInfo, now, expiresAt, false);

        try {
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(KEY_PREFIX + tokenId, json, ttl);
            redisTemplate.opsForSet().add(USER_INDEX_PREFIX + userId, tokenId);
            redisTemplate.expire(USER_INDEX_PREFIX + userId, ttl.plusDays(1));
        } catch (Exception e) {
            log.error("Failed to serialize and store refresh token in Redis: tokenId={}", tokenId, e);
            throw new IllegalStateException("Redis session storage error", e);
        }
    }

    @Override
    public Optional<RefreshTokenSession> find(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return Optional.empty();
        }

        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + tokenId);
            if (json != null && !json.isBlank()) {
                RefreshTokenSession session = objectMapper.readValue(json, RefreshTokenSession.class);
                if (session.isExpired()) {
                    revoke(tokenId);
                    return Optional.empty();
                }
                return Optional.of(session);
            }

            // Check if this token was previously consumed (Token Reuse Detection)
            String consumedUserId = redisTemplate.opsForValue().get(CONSUMED_PREFIX + tokenId);
            if (consumedUserId != null && !consumedUserId.isBlank()) {
                long now = System.currentTimeMillis();
                RefreshTokenSession tombstone = new RefreshTokenSession(
                        tokenId,
                        UUID.fromString(consumedUserId),
                        "Unknown",
                        now,
                        now + 60000,
                        true
                );
                return Optional.of(tombstone);
            }

            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to retrieve or deserialize refresh token from Redis: tokenId={}", tokenId, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean rotate(String oldTokenId, String newTokenId, Duration ttl) {
        try {
            // Atomic GETDEL in Redis 6.2+
            String json = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + oldTokenId);
            if (json == null || json.isBlank()) {
                log.warn("Security Alert: Token reuse or invalid token presented for rotation: tokenId={}", oldTokenId);
                return false;
            }

            RefreshTokenSession oldSession = objectMapper.readValue(json, RefreshTokenSession.class);
            UUID userId = oldSession.userId();

            redisTemplate.opsForSet().remove(USER_INDEX_PREFIX + userId, oldTokenId);

            // Record consumed tombstone to detect future replays
            redisTemplate.opsForValue().set(CONSUMED_PREFIX + oldTokenId, userId.toString(), ttl);

            store(newTokenId, userId, oldSession.deviceInfo(), ttl);
            return true;
        } catch (Exception e) {
            log.error("Failed to execute atomic token rotation in Redis: oldTokenId={}", oldTokenId, e);
            return false;
        }
    }

    @Override
    public void revoke(String tokenId) {
        try {
            String json = redisTemplate.opsForValue().getAndDelete(KEY_PREFIX + tokenId);
            if (json != null && !json.isBlank()) {
                RefreshTokenSession session = objectMapper.readValue(json, RefreshTokenSession.class);
                redisTemplate.opsForSet().remove(USER_INDEX_PREFIX + session.userId(), tokenId);
            }
            redisTemplate.delete(CONSUMED_PREFIX + tokenId);
        } catch (Exception e) {
            log.error("Failed to revoke refresh token from Redis: tokenId={}", tokenId, e);
        }
    }

    @Override
    public void revokeAllForUser(UUID userId) {
        try {
            Set<String> tokens = redisTemplate.opsForSet().members(USER_INDEX_PREFIX + userId);
            if (tokens != null && !tokens.isEmpty()) {
                for (String tokenId : tokens) {
                    redisTemplate.delete(KEY_PREFIX + tokenId);
                    redisTemplate.delete(CONSUMED_PREFIX + tokenId);
                }
            }
            redisTemplate.delete(USER_INDEX_PREFIX + userId);
            log.info("Revoked all active tokens for userId={} in Redis", userId);
        } catch (Exception e) {
            log.error("Failed to revoke all user sessions from Redis: userId={}", userId, e);
        }
    }
}
