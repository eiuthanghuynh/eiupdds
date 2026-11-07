package com.eiu.pdds.auth.service;

import com.eiu.pdds.auth.model.RefreshTokenSession;
import com.eiu.pdds.auth.service.impl.InMemoryRefreshTokenStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRefreshTokenStoreTest {

    private InMemoryRefreshTokenStore store;
    private UUID userId;

    @BeforeEach
    void setUp() {
        store = new InMemoryRefreshTokenStore();
        userId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Should store and retrieve active refresh token session")
    void testStoreAndFind() {
        store.store("token-123", userId, "iPhone 15", Duration.ofMinutes(10));

        Optional<RefreshTokenSession> session = store.find("token-123");
        assertThat(session).isPresent();
        assertThat(session.get().userId()).isEqualTo(userId);
        assertThat(session.get().deviceInfo()).isEqualTo("iPhone 15");
    }

    @Test
    @DisplayName("Should return empty for expired token and evict it")
    void testExpiredToken() throws InterruptedException {
        store.store("short-lived-token", userId, "Test Device", Duration.ofMillis(5));
        Thread.sleep(15);

        Optional<RefreshTokenSession> session = store.find("short-lived-token");
        assertThat(session).isEmpty();
    }

    @Test
    @DisplayName("Should rotate token atomically: invalidate old token and register new token")
    void testRotateToken() {
        store.store("old-token", userId, "Pixel 8", Duration.ofMinutes(30));

        boolean rotated = store.rotate("old-token", "new-token", Duration.ofMinutes(30));
        assertThat(rotated).isTrue();

        assertThat(store.find("old-token")).isPresent();
        assertThat(store.find("old-token").get().isConsumed()).isTrue();
        assertThat(store.find("new-token")).isPresent();
        assertThat(store.find("new-token").get().isConsumed()).isFalse();
        assertThat(store.find("new-token").get().userId()).isEqualTo(userId);

        // Subsequent rotation attempt with consumed token must fail
        boolean secondRotation = store.rotate("old-token", "another-token", Duration.ofMinutes(30));
        assertThat(secondRotation).isFalse();
    }

    @Test
    @DisplayName("Should return false when attempting to rotate non-existent or already rotated token (Reuse Detection)")
    void testRotateNonExistentToken() {
        boolean rotated = store.rotate("non-existent-token", "new-token", Duration.ofMinutes(30));
        assertThat(rotated).isFalse();
    }

    @Test
    @DisplayName("Should revoke specific token")
    void testRevokeToken() {
        store.store("token-to-delete", userId, "Web", Duration.ofHours(1));
        store.revoke("token-to-delete");

        assertThat(store.find("token-to-delete")).isEmpty();
    }

    @Test
    @DisplayName("Should revoke all active tokens bound to a user on security breach")
    void testRevokeAllForUser() {
        store.store("token-device-1", userId, "Mobile", Duration.ofDays(7));
        store.store("token-device-2", userId, "Tablet", Duration.ofDays(7));

        store.revokeAllForUser(userId);

        assertThat(store.find("token-device-1")).isEmpty();
        assertThat(store.find("token-device-2")).isEmpty();
    }
}
