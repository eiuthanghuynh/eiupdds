package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.diagnosis.service.impl.RedisDiagnosisIdempotencyService;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import com.eiu.pdds.dto.plant.PlantDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisDiagnosisIdempotencyServiceTest {

    private static final int TTL_MINUTES = 5;
    private static final Duration EXPECTED_TTL = Duration.ofMinutes(TTL_MINUTES);

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ObjectMapper objectMapper;
    private RedisDiagnosisIdempotencyService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new RedisDiagnosisIdempotencyService(redisTemplate, objectMapper, TTL_MINUTES);
    }

    private DiagnosisAnalyzeResponse createSampleResponse(UUID diagnosisId) {
        return new DiagnosisAnalyzeResponse(
                diagnosisId,
                "https://s3.eiu.edu.vn/eiu-pdds-plants/diagnoses/sample.jpg",
                400,
                500,
                new PlantDto(UUID.randomUUID(), "POTATO", "Potato", "Solanum tuberosum", null, null),
                new DiseaseSummaryDto(
                        UUID.randomUUID(),
                        "POTATO_LATE_BLIGHT",
                        "Potato leaf late blight",
                        "Phytophthora infestans",
                        Severity.CRITICAL
                ),
                0.89,
                List.of(),
                List.of(),
                List.of(),
                145,
                11.0528,
                106.6661,
                "iPhone 15 Pro",
                "COMPLETED",
                Instant.parse("2026-11-20T10:15:30Z")
        );
    }

    @Test
    @DisplayName("Authenticated user recordDiagnosis + findRecentDiagnosis uses scoped key, reverse index, and exact TTL")
    void recordAndFind_authenticatedUser_storesAndReplaysWithExactKeysAndTtl() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID diagnosisId = UUID.randomUUID();
        String hash = "abc123sha256";
        String expectedPrimaryKey = "pdds:idem:diag:" + userId + ":" + hash;
        String expectedReverseKey = "pdds:idem:diag-idx:" + diagnosisId;
        DiagnosisAnalyzeResponse response = createSampleResponse(diagnosisId);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service.recordDiagnosis(userId, hash, response);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(eq(expectedPrimaryKey), jsonCaptor.capture(), eq(EXPECTED_TTL));
        verify(valueOperations).set(eq(expectedReverseKey), eq(expectedPrimaryKey), eq(EXPECTED_TTL));

        when(valueOperations.get(expectedPrimaryKey)).thenReturn(jsonCaptor.getValue());

        Optional<DiagnosisAnalyzeResponse> cached = service.findRecentDiagnosis(userId, hash);
        assertThat(cached).isPresent();
        assertThat(cached.get().diagnosisId()).isEqualTo(diagnosisId);
        assertThat(cached.get().dominantDisease().diseaseCode()).isEqualTo("POTATO_LATE_BLIGHT");
        assertThat(cached.get().latitude()).isEqualTo(11.0528);
        assertThat(cached.get().diagnosedAt()).isEqualTo(Instant.parse("2026-11-20T10:15:30Z"));
    }

    @Test
    @DisplayName("Guest (userId == null) must never read, write, or evict from Redis")
    void guestOperations_userIdNull_neverInteractWithRedis() {
        String hash = "guest_image_hash";
        DiagnosisAnalyzeResponse response = createSampleResponse(UUID.randomUUID());

        Optional<DiagnosisAnalyzeResponse> found = service.findRecentDiagnosis(null, hash);
        service.recordDiagnosis(null, hash, response);
        service.evict(null, hash);

        assertThat(found).isEmpty();
        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("Null or blank imageHash / null response / null diagnosisId safely no-op without touching Redis")
    void nullAndBlankGuards_neverInteractWithRedis() {
        UUID userId = UUID.randomUUID();
        DiagnosisAnalyzeResponse responseWithNullId = createSampleResponse(null);

        assertThat(service.findRecentDiagnosis(userId, null)).isEmpty();
        assertThat(service.findRecentDiagnosis(userId, "   ")).isEmpty();

        service.recordDiagnosis(userId, null, createSampleResponse(UUID.randomUUID()));
        service.recordDiagnosis(userId, "  ", createSampleResponse(UUID.randomUUID()));
        service.recordDiagnosis(userId, "valid-hash", null);
        service.recordDiagnosis(userId, "valid-hash", responseWithNullId);

        service.evict(userId, null);
        service.evict(userId, "");
        service.evictByDiagnosisId(null);
        service.evictById(null);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    @DisplayName("evict(userId, hash) deletes scoped primary key safely")
    void evict_authenticatedUser_deletesPrimaryKey() {
        UUID userId = UUID.randomUUID();
        String hash = "hash_to_evict";
        String expectedPrimaryKey = "pdds:idem:diag:" + userId + ":" + hash;

        service.evict(userId, hash);

        verify(redisTemplate).delete(expectedPrimaryKey);
    }

    @Test
    @DisplayName("evictByDiagnosisId performs O(1) reverse-index lookup and batch-deletes primary and reverse keys")
    void evictByDiagnosisId_foundReverseIndex_deletesPrimaryAndReverseKeysInBatch() {
        UUID userId = UUID.randomUUID();
        UUID diagnosisId = UUID.randomUUID();
        String primaryKey = "pdds:idem:diag:" + userId + ":somehash";
        String reverseKey = "pdds:idem:diag-idx:" + diagnosisId;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(reverseKey)).thenReturn(primaryKey);

        service.evictByDiagnosisId(diagnosisId);

        verify(redisTemplate).delete(List.of(primaryKey, reverseKey));
    }

    @Test
    @DisplayName("evictById delegates to evictByDiagnosisId")
    void evictById_delegatesToEvictByDiagnosisId() {
        UUID userId = UUID.randomUUID();
        UUID diagnosisId = UUID.randomUUID();
        String primaryKey = "pdds:idem:diag:" + userId + ":alias";
        String reverseKey = "pdds:idem:diag-idx:" + diagnosisId;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(reverseKey)).thenReturn(primaryKey);

        service.evictById(diagnosisId);

        verify(redisTemplate).delete(List.of(primaryKey, reverseKey));
    }

    @Test
    @DisplayName("evictByDiagnosisId safely no-ops without calling delete when reverse key is missing or expired")
    void evictByDiagnosisId_missingReverseIndex_doesNotCallDelete() {
        UUID diagnosisId = UUID.randomUUID();
        String reverseKey = "pdds:idem:diag-idx:" + diagnosisId;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(reverseKey)).thenReturn(null);

        assertThatCode(() -> service.evictByDiagnosisId(diagnosisId)).doesNotThrowAnyException();

        verify(redisTemplate, never()).delete(anyString());
        verify(redisTemplate, never()).delete(anyCollection());
    }

    @Test
    @DisplayName("Redis connection failure fails open across get, set, and delete without throwing")
    void redisConnectionFailure_failsOpenGracefullyAcrossAllOperations() {
        UUID userId = UUID.randomUUID();
        UUID diagnosisId = UUID.randomUUID();
        String hash = "outage_hash";
        String primaryKey = "pdds:idem:diag:" + userId + ":" + hash;
        String reverseKey = "pdds:idem:diag-idx:" + diagnosisId;
        DiagnosisAnalyzeResponse response = createSampleResponse(diagnosisId);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(primaryKey)).thenThrow(new RedisConnectionFailureException("Redis down"));
        doThrow(new RedisConnectionFailureException("Redis down"))
                .when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        when(redisTemplate.delete(primaryKey)).thenThrow(new RedisConnectionFailureException("Redis down"));
        when(valueOperations.get(reverseKey)).thenThrow(new RedisConnectionFailureException("Redis down"));

        assertThat(service.findRecentDiagnosis(userId, hash)).isEmpty();
        assertThatCode(() -> service.recordDiagnosis(userId, hash, response)).doesNotThrowAnyException();
        assertThatCode(() -> service.evict(userId, hash)).doesNotThrowAnyException();
        assertThatCode(() -> service.evictByDiagnosisId(diagnosisId)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Corrupted JSON payload in Redis fails open and returns Optional.empty()")
    void findRecentDiagnosis_corruptedJson_failsOpenReturnsEmpty() {
        UUID userId = UUID.randomUUID();
        String hash = "corrupt_hash";
        String primaryKey = "pdds:idem:diag:" + userId + ":" + hash;

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(primaryKey)).thenReturn("{not-valid-json");

        Optional<DiagnosisAnalyzeResponse> result = service.findRecentDiagnosis(userId, hash);

        assertThat(result).isEmpty();
    }
}
