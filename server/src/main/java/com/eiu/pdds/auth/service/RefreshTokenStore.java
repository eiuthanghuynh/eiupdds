package com.eiu.pdds.auth.service;

import com.eiu.pdds.auth.model.RefreshTokenSession;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for Refresh Token lifecycle, rotation, and revocation storage (DIP & OCP).
 */
public interface RefreshTokenStore {

    /**
     * Persists a new active refresh token session with designated TTL.
     */
    void store(String tokenId, UUID userId, String deviceInfo, Duration ttl);

    /**
     * Finds active session by tokenId.
     */
    Optional<RefreshTokenSession> find(String tokenId);

    /**
     * Performs atomic token rotation: consumes old tokenId and records new tokenId.
     * If oldTokenId is missing/already rotated, detects Token Reuse Attack.
     *
     * @return true if rotation succeeded, false if reuse detected
     */
    boolean rotate(String oldTokenId, String newTokenId, Duration ttl);

    /**
     * Revokes a single token session upon standard logout.
     */
    void revoke(String tokenId);

    /**
     * Revokes all active token sessions belonging to the user (e.g. password reset or reuse detection).
     */
    void revokeAllForUser(UUID userId);
}
