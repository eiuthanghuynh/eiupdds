package com.eiu.pdds.auth.service.impl;

import com.eiu.pdds.auth.model.RefreshTokenSession;
import com.eiu.pdds.auth.service.RefreshTokenStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe In-Memory implementation of RefreshTokenStore for tests and standalone operation.
 * Implements Token Reuse Detection via tombstoning consumed tokens.
 */
@Slf4j
@Component
@Profile("test")
public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private final ConcurrentHashMap<String, RefreshTokenSession> tokenMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<String>> userTokenIndex = new ConcurrentHashMap<>();

    @Override
    public void store(String tokenId, UUID userId, String deviceInfo, Duration ttl) {
        long now = System.currentTimeMillis();
        long expiresAt = now + ttl.toMillis();
        RefreshTokenSession session = new RefreshTokenSession(tokenId, userId, deviceInfo, now, expiresAt, false);
        tokenMap.put(tokenId, session);
        userTokenIndex.computeIfAbsent(userId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(tokenId);
    }

    @Override
    public Optional<RefreshTokenSession> find(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return Optional.empty();
        }
        RefreshTokenSession session = tokenMap.get(tokenId);
        if (session != null && session.isExpired()) {
            tokenMap.remove(tokenId);
            return Optional.empty();
        }
        return Optional.ofNullable(session);
    }

    @Override
    public boolean rotate(String oldTokenId, String newTokenId, Duration ttl) {
        RefreshTokenSession oldSession = tokenMap.get(oldTokenId);
        if (oldSession == null || oldSession.isExpired() || oldSession.isConsumed()) {
            log.warn("Token reuse detected or invalid token presented for rotation: tokenId={}", oldTokenId);
            return false;
        }

        // Tombstone old token as consumed to detect replay attacks
        RefreshTokenSession consumedSession = new RefreshTokenSession(
                oldSession.tokenId(),
                oldSession.userId(),
                oldSession.deviceInfo(),
                oldSession.createdAtEpochMs(),
                oldSession.expiresAtEpochMs(),
                true
        );
        tokenMap.put(oldTokenId, consumedSession);

        userTokenIndex.computeIfPresent(oldSession.userId(), (uid, tokens) -> {
            tokens.remove(oldTokenId);
            return tokens;
        });

        store(newTokenId, oldSession.userId(), oldSession.deviceInfo(), ttl);
        return true;
    }

    @Override
    public void revoke(String tokenId) {
        RefreshTokenSession session = tokenMap.remove(tokenId);
        if (session != null) {
            userTokenIndex.computeIfPresent(session.userId(), (uid, tokens) -> {
                tokens.remove(tokenId);
                return tokens;
            });
        }
    }

    @Override
    public void revokeAllForUser(UUID userId) {
        Set<String> tokens = userTokenIndex.remove(userId);
        if (tokens != null) {
            tokens.forEach(tokenMap::remove);
        }
        tokenMap.values().removeIf(s -> s.userId().equals(userId));
        log.info("Revoked all active and consumed tokens for userId={}", userId);
    }
}
