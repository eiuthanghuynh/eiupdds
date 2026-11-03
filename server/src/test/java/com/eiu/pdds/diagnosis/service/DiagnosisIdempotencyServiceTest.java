package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.diagnosis.service.impl.InMemoryDiagnosisIdempotencyService;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import com.eiu.pdds.disease.entity.Severity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosisIdempotencyServiceTest {

    private DiagnosisIdempotencyService idempotencyService;

    @BeforeEach
    void setUp() {
        idempotencyService = new InMemoryDiagnosisIdempotencyService(5);
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
                Instant.now()
        );
    }

    @Test
    @DisplayName("Should return empty when hash has not been recorded")
    void testFindRecentDiagnosis_CacheMiss() {
        Optional<DiagnosisAnalyzeResponse> cached = idempotencyService.findRecentDiagnosis("non_existent_hash");
        assertThat(cached).isEmpty();
    }

    @Test
    @DisplayName("Should record and retrieve diagnosis response for identical image hash (Idempotent hit)")
    void testRecordAndRetrieve_CacheHit() {
        String hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        UUID diagnosisId = UUID.randomUUID();
        DiagnosisAnalyzeResponse response = createDummyResponse(diagnosisId);

        idempotencyService.recordDiagnosis(hash, response);

        Optional<DiagnosisAnalyzeResponse> cached = idempotencyService.findRecentDiagnosis(hash);
        assertThat(cached).isPresent();
        assertThat(cached.get().diagnosisId()).isEqualTo(diagnosisId);
        assertThat(cached.get().dominantDisease().diseaseCode()).isEqualTo("POTATO_LATE_BLIGHT");
    }

    @Test
    @DisplayName("Should evict entry explicitly")
    void testEvict() {
        String hash = "sample_hash_to_evict";
        idempotencyService.recordDiagnosis(hash, createDummyResponse(UUID.randomUUID()));

        assertThat(idempotencyService.findRecentDiagnosis(hash)).isPresent();

        idempotencyService.evict(hash);

        assertThat(idempotencyService.findRecentDiagnosis(hash)).isEmpty();
    }
}
