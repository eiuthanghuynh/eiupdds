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
    public Optional<DiagnosisAnalyzeResponse> findRecentDiagnosis(String imageHash) {
        if (imageHash == null || imageHash.isBlank()) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        CacheEntry entry = cache.get(imageHash);

        if (entry == null) {
            return Optional.empty();
        }

        if (entry.isExpired(now)) {
            cache.remove(imageHash);
            log.debug("Evicted expired idempotency cache entry for hash='{}'", imageHash);
            return Optional.empty();
        }

        return Optional.of(entry.response());
    }

    @Override
    public void recordDiagnosis(String imageHash, DiagnosisAnalyzeResponse response) {
        if (imageHash == null || imageHash.isBlank() || response == null) {
            return;
        }

        // Opportunistic cleanup if cache size exceeds threshold
        if (cache.size() > 1000) {
            cleanExpiredEntries();
        }

        Instant expiresAt = Instant.now().plus(ttl);
        cache.put(imageHash, new CacheEntry(response, expiresAt));
        log.debug("Recorded idempotency cache entry for hash='{}', ttl={}", imageHash, ttl);
    }

    @Override
    public void evict(String imageHash) {
        if (imageHash != null) {
            cache.remove(imageHash);
        }
    }

    private void cleanExpiredEntries() {
        Instant now = Instant.now();
        cache.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }
}
