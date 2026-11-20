package com.eiu.pdds.diagnosis.service.impl;

import com.eiu.pdds.diagnosis.service.DiagnosisIdempotencyService;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Distributed Redis-backed diagnosis idempotency store for non-test environments.
 * Enforces strict user scoping (guests are never cached to prevent cross-guest data leaks)
 * and fails open on Redis outages or serialization anomalies.
 */
@Slf4j
@Service
@Primary
@Profile("!test")
public class RedisDiagnosisIdempotencyService implements DiagnosisIdempotencyService {

    private static final String PRIMARY_KEY_PREFIX = "pdds:idem:diag:";
    private static final String REVERSE_INDEX_PREFIX = "pdds:idem:diag-idx:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    @Autowired
    public RedisDiagnosisIdempotencyService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${diagnosis.idempotency.ttl-minutes:5}") int ttlMinutes
    ) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.ttl = Duration.ofMinutes(Math.max(1, ttlMinutes));
    }

    @Override
    public Optional<DiagnosisAnalyzeResponse> findRecentDiagnosis(UUID userId, String imageHash) {
        if (userId == null || imageHash == null || imageHash.isBlank()) {
            return Optional.empty();
        }

        String primaryKey = buildPrimaryKey(userId, imageHash);
        try {
            String json = redisTemplate.opsForValue().get(primaryKey);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(objectMapper.readValue(json, DiagnosisAnalyzeResponse.class));
        } catch (Exception e) {
            log.warn("Diagnosis idempotency lookup failed (fail-open) for key='{}': {}", primaryKey, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void recordDiagnosis(UUID userId, String imageHash, DiagnosisAnalyzeResponse response) {
        if (userId == null || imageHash == null || imageHash.isBlank()
                || response == null || response.diagnosisId() == null) {
            return;
        }

        String primaryKey = buildPrimaryKey(userId, imageHash);
        String reverseKey = buildReverseIndexKey(response.diagnosisId());
        try {
            String json = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(primaryKey, json, ttl);
            redisTemplate.opsForValue().set(reverseKey, primaryKey, ttl);
            log.debug("Recorded Redis diagnosis idempotency entry for key='{}', ttl={}", primaryKey, ttl);
        } catch (Exception e) {
            log.warn("Diagnosis idempotency record failed (fail-open) for key='{}': {}", primaryKey, e.getMessage());
        }
    }

    @Override
    public void evict(UUID userId, String imageHash) {
        if (userId == null || imageHash == null || imageHash.isBlank()) {
            return;
        }

        String primaryKey = buildPrimaryKey(userId, imageHash);
        try {
            redisTemplate.delete(primaryKey);
        } catch (Exception e) {
            log.warn("Diagnosis idempotency evict failed (fail-open) for key='{}': {}", primaryKey, e.getMessage());
        }
    }

    @Override
    public void evictByDiagnosisId(UUID diagnosisId) {
        if (diagnosisId == null) {
            return;
        }

        String reverseKey = buildReverseIndexKey(diagnosisId);
        try {
            String primaryKey = redisTemplate.opsForValue().get(reverseKey);
            if (primaryKey != null && !primaryKey.isBlank()) {
                redisTemplate.delete(List.of(primaryKey, reverseKey));
                log.debug("Evicted Redis idempotency entry for diagnosisId='{}'", diagnosisId);
            }
        } catch (Exception e) {
            log.warn("Diagnosis idempotency evictByDiagnosisId failed (fail-open) for diagnosisId='{}': {}",
                    diagnosisId, e.getMessage());
        }
    }

    @Override
    public void evictById(UUID diagnosisId) {
        evictByDiagnosisId(diagnosisId);
    }

    private String buildPrimaryKey(UUID userId, String imageHash) {
        return PRIMARY_KEY_PREFIX + userId + ":" + imageHash;
    }

    private String buildReverseIndexKey(UUID diagnosisId) {
        return REVERSE_INDEX_PREFIX + diagnosisId;
    }
}
