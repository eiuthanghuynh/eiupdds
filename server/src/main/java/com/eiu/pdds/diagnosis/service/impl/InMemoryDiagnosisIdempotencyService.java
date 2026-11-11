package com.eiu.pdds.diagnosis.service.impl;

import com.eiu.pdds.diagnosis.service.DiagnosisIdempotencyService;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance, in-memory thread-safe implementation of DiagnosisIdempotencyService (SRP & DIP).
 * Enforces sliding window TTL eviction to protect Roboflow API quota while saving network overhead.
 */
@Slf4j
@Service
public class InMemoryDiagnosisIdempotencyService implements DiagnosisIdempotencyService {

    private record CacheEntry(DiagnosisAnalyzeResponse response, Instant expiresAt) {
        boolean isExpired(Instant now) {
            return now.isAfter(expiresAt);
        }
    }

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Duration ttl;

    public InMemoryDiagnosisIdempotencyService(
            @Value("${diagnosis.idempotency.ttl-minutes:5}") int ttlMinutes
    ) {
        this.ttl = Duration.ofMinutes(Math.max(1, ttlMinutes));
    }

    @Override
    public Optional<DiagnosisAnalyzeResponse> findRecentDiagnosis(UUID userId, String imageHash) {
        if (imageHash == null || imageHash.isBlank()) {
            return Optional.empty();
        }

        String key = buildKey(userId, imageHash);
        Instant now = Instant.now();
        CacheEntry entry = cache.get(key);

        if (entry == null) {
            return Optional.empty();
        }

        if (entry.isExpired(now)) {
            cache.remove(key);
            log.debug("Evicted expired idempotency cache entry for key='{}'", key);
            return Optional.empty();
        }

        return Optional.of(entry.response());
    }

    @Override
    public void recordDiagnosis(UUID userId, String imageHash, DiagnosisAnalyzeResponse response) {
        if (imageHash == null || imageHash.isBlank() || response == null) {
            return;
        }

        if (cache.size() > 1000) {
            cleanExpiredEntries();
        }

        String key = buildKey(userId, imageHash);
        Instant expiresAt = Instant.now().plus(ttl);
        cache.put(key, new CacheEntry(response, expiresAt));
        log.debug("Recorded idempotency cache entry for key='{}', ttl={}", key, ttl);
    }

    @Override
    public void evict(UUID userId, String imageHash) {
        if (imageHash != null) {
            cache.remove(buildKey(userId, imageHash));
        }
    }

    @Override
    public void evictByDiagnosisId(UUID diagnosisId) {
        if (diagnosisId != null) {
            cache.entrySet().removeIf(entry ->
                    entry.getValue().response() != null &&
                    diagnosisId.equals(entry.getValue().response().diagnosisId()));
        }
    }

    private String buildKey(UUID userId, String imageHash) {
        return (userId != null ? userId.toString() : "anon") + ":" + imageHash;
    }

    private void cleanExpiredEntries() {
        Instant now = Instant.now();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }
}
