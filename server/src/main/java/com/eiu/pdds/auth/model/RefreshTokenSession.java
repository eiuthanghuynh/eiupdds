package com.eiu.pdds.auth.model;

import java.io.Serializable;
import java.util.UUID;

/**
 * Immutable session metadata bound to a persistent Refresh Token.
 */
public record RefreshTokenSession(
        String tokenId,
        UUID userId,
        String deviceInfo,
        long createdAtEpochMs,
        long expiresAtEpochMs,
        boolean isConsumed
) implements Serializable {

    public RefreshTokenSession(String tokenId, UUID userId, String deviceInfo, long createdAtEpochMs, long expiresAtEpochMs) {
        this(tokenId, userId, deviceInfo, createdAtEpochMs, expiresAtEpochMs, false);
    }

    public boolean isExpired() {
        return System.currentTimeMillis() >= expiresAtEpochMs;
    }
}
