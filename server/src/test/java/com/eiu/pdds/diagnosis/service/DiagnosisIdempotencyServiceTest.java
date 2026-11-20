package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.diagnosis.service.impl.InMemoryDiagnosisIdempotencyService;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosisIdempotencyServiceTest {

    private MutableClock clock;
    private InMemoryDiagnosisIdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-20T10:00:00Z"));
        idempotencyService = new InMemoryDiagnosisIdempotencyService(5, clock);
    }

    private DiagnosisAnalyzeResponse createDummyResponse(UUID id) {
        return new DiagnosisAnalyzeResponse(
                id,
                "http://s3/test.jpg",
                400,
                500,
                new DiseaseSummaryDto(UUID.randomUUID(), "POTATO_LATE_BLIGHT", "Potato late blight", "Phytophthora infestans", Severity.CRITICAL),
                0.85,
                List.of(),
                List.of(),
                List.of(),
                120,
                "COMPLETED",
                Instant.now(clock)
        );
    }

    @Test
    @DisplayName("Should return empty when hash has not been recorded for authenticated user")
    void testFindRecentDiagnosis_CacheMiss() {
        UUID userId = UUID.randomUUID();
        Optional<DiagnosisAnalyzeResponse> cached = idempotencyService.findRecentDiagnosis(userId, "non_existent_hash");
        assertThat(cached).isEmpty();
    }

    @Test
    @DisplayName("Should record and retrieve diagnosis response for identical image hash of same authenticated user")
    void testRecordAndRetrieve_CacheHit() {
        UUID userId = UUID.randomUUID();
        String hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        UUID diagnosisId = UUID.randomUUID();
        DiagnosisAnalyzeResponse response = createDummyResponse(diagnosisId);

        idempotencyService.recordDiagnosis(userId, hash, response);

        Optional<DiagnosisAnalyzeResponse> cached = idempotencyService.findRecentDiagnosis(userId, hash);
        assertThat(cached).isPresent();
        assertThat(cached.get().diagnosisId()).isEqualTo(diagnosisId);
        assertThat(cached.get().dominantDisease().diseaseCode()).isEqualTo("POTATO_LATE_BLIGHT");
    }

    @Test
    @DisplayName("Should isolate cache entries across different authenticated users for identical image hash")
    void testCrossUserIsolation_UserBReceivesCacheMiss() {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();
        String sharedHash = "shared_leaf_hash_sha256";
        UUID diagnosisIdA = UUID.randomUUID();

        idempotencyService.recordDiagnosis(userA, sharedHash, createDummyResponse(diagnosisIdA));

        assertThat(idempotencyService.findRecentDiagnosis(userA, sharedHash)).isPresent();
        assertThat(idempotencyService.findRecentDiagnosis(userB, sharedHash)).isEmpty();
    }

    @Test
    @DisplayName("Guest (userId == null) must never hit or populate idempotency cache")
    void testGuestIsolation_NeverCachedOrReplayed() {
        UUID userA = UUID.randomUUID();
        String hash = "guest_shared_hash";
        DiagnosisAnalyzeResponse guestResponse = createDummyResponse(UUID.randomUUID());
        DiagnosisAnalyzeResponse userAResponse = createDummyResponse(UUID.randomUUID());

        idempotencyService.recordDiagnosis(null, hash, guestResponse);
        assertThat(idempotencyService.findRecentDiagnosis(null, hash)).isEmpty();

        // Recording for User A must remain isolated from guest lookups and guest evictions
        idempotencyService.recordDiagnosis(userA, hash, userAResponse);
        assertThat(idempotencyService.findRecentDiagnosis(null, hash)).isEmpty();

        idempotencyService.evict(null, hash);
        assertThat(idempotencyService.findRecentDiagnosis(userA, hash)).isPresent();
    }

    @Test
    @DisplayName("Should evict entry explicitly for authenticated user")
    void testEvict() {
        UUID userId = UUID.randomUUID();
        String hash = "sample_hash_to_evict";
        idempotencyService.recordDiagnosis(userId, hash, createDummyResponse(UUID.randomUUID()));

        assertThat(idempotencyService.findRecentDiagnosis(userId, hash)).isPresent();

        idempotencyService.evict(userId, hash);

        assertThat(idempotencyService.findRecentDiagnosis(userId, hash)).isEmpty();
    }

    @Test
    @DisplayName("Should evict entry in O(1) by diagnosisId via reverse index")
    void testEvictByDiagnosisId() {
        UUID userId = UUID.randomUUID();
        String hash = "sample_hash_to_evict_by_id";
        UUID diagnosisId = UUID.randomUUID();
        idempotencyService.recordDiagnosis(userId, hash, createDummyResponse(diagnosisId));

        assertThat(idempotencyService.findRecentDiagnosis(userId, hash)).isPresent();

        idempotencyService.evictByDiagnosisId(diagnosisId);

        assertThat(idempotencyService.findRecentDiagnosis(userId, hash)).isEmpty();
    }

    @Test
    @DisplayName("Overwriting same key with new diagnosisId cleans stale reverse index so evicting old ID does not drop new entry")
    void testOverwriteCleansStaleReverseIndex() {
        UUID userId = UUID.randomUUID();
        String hash = "overwritten_hash";
        UUID oldId = UUID.randomUUID();
        UUID newId = UUID.randomUUID();

        idempotencyService.recordDiagnosis(userId, hash, createDummyResponse(oldId));
        idempotencyService.recordDiagnosis(userId, hash, createDummyResponse(newId));

        idempotencyService.evictByDiagnosisId(oldId);

        Optional<DiagnosisAnalyzeResponse> remaining = idempotencyService.findRecentDiagnosis(userId, hash);
        assertThat(remaining).isPresent();
        assertThat(remaining.get().diagnosisId()).isEqualTo(newId);
    }

    @Test
    @DisplayName("Expired entry returns empty and clear() resets all cached entries")
    void testExpirationAndClear() {
        UUID userId = UUID.randomUUID();
        String hash = "expiring_hash";
        UUID diagnosisId = UUID.randomUUID();

        idempotencyService.recordDiagnosis(userId, hash, createDummyResponse(diagnosisId));
        assertThat(idempotencyService.findRecentDiagnosis(userId, hash)).isPresent();

        clock.advance(Duration.ofMinutes(6));
        assertThat(idempotencyService.findRecentDiagnosis(userId, hash)).isEmpty();

        idempotencyService.recordDiagnosis(userId, hash, createDummyResponse(UUID.randomUUID()));
        assertThat(idempotencyService.findRecentDiagnosis(userId, hash)).isPresent();

        idempotencyService.clear();
        assertThat(idempotencyService.findRecentDiagnosis(userId, hash)).isEmpty();
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> current;

        private MutableClock(Instant initial) {
            this.current = new AtomicReference<>(initial);
        }

        void advance(Duration duration) {
            current.updateAndGet(t -> t.plus(duration));
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
            return current.get();
        }
    }
}
