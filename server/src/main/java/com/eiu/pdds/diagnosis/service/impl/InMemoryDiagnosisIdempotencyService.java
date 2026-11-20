package com.eiu.pdds.diagnosis.service.impl;

import com.eiu.pdds.diagnosis.service.DiagnosisIdempotencyService;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory diagnosis idempotency store used exclusively under the 'test' profile.
 * Mirrors RedisDiagnosisIdempotencyService semantics: guests (userId == null) are never cached,
 * and a reverse index supports O(1) eviction by diagnosisId.
 */
@Slf4j
@Service
@Profile("test")
public class InMemoryDiagnosisIdempotencyService implements DiagnosisIdempotencyService {

    private record CacheEntry(DiagnosisAnalyzeResponse response, Instant expiresAt) {
        boolean isExpired(Instant now) {
            return now.isAfter(expiresAt);
        }
    }

    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> diagnosisIdToKey = new ConcurrentHashMap<>();
    private final Duration ttl;
    private final Clock clock;

    @Autowired
    public InMemoryDiagnosisIdempotencyService(
            @Value("${diagnosis.idempotency.ttl-minutes:5}") int ttlMinutes
    ) {
        this(ttlMinutes, Clock.systemUTC());
    }

    public InMemoryDiagnosisIdempotencyService(int ttlMinutes, Clock clock) {
        this.ttl = Duration.ofMinutes(Math.max(1, ttlMinutes));
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public Optional<DiagnosisAnalyzeResponse> findRecentDiagnosis(UUID userId, String imageHash) {
        if (userId == null || imageHash == null || imageHash.isBlank()) {
            return Optional.empty();
        }

        String key = buildKey(userId, imageHash);
        Instant now = Instant.now(clock);
        CacheEntry entry = cache.get(key);

        if (entry == null) {
            return Optional.empty();
        }

        if (entry.isExpired(now)) {
            if (cache.remove(key, entry)) {
                removeReverseIndex(entry, key);
            }
            log.debug("Evicted expired idempotency cache entry for key='{}'", key);
            return Optional.empty();
        }

        return Optional.of(entry.response());
    }

    @Override
    public void recordDiagnosis(UUID userId, String imageHash, DiagnosisAnalyzeResponse response) {
        if (userId == null || imageHash == null || imageHash.isBlank()
                || response == null || response.diagnosisId() == null) {
            return;
        }

        if (cache.size() > 1000) {
            cleanExpiredEntries();
        }

        String key = buildKey(userId, imageHash);
        Instant expiresAt = Instant.now(clock).plus(ttl);
        CacheEntry newEntry = new CacheEntry(response, expiresAt);

        CacheEntry previous = cache.put(key, newEntry);
        if (previous != null && previous.response() != null && previous.response().diagnosisId() != null) {
            UUID previousDiagnosisId = previous.response().diagnosisId();
            if (!previousDiagnosisId.equals(response.diagnosisId())) {
                diagnosisIdToKey.remove(previousDiagnosisId, key);
            }
        }
        diagnosisIdToKey.put(response.diagnosisId(), key);
        log.debug("Recorded idempotency cache entry for key='{}', ttl={}", key, ttl);
    }

    @Override
    public void evict(UUID userId, String imageHash) {
        if (userId == null || imageHash == null || imageHash.isBlank()) {
            return;
        }

        String key = buildKey(userId, imageHash);
        CacheEntry removed = cache.remove(key);
        if (removed != null) {
            removeReverseIndex(removed, key);
        }
    }

    @Override
    public void evictByDiagnosisId(UUID diagnosisId) {
        if (diagnosisId == null) {
            return;
        }

        String key = diagnosisIdToKey.remove(diagnosisId);
        if (key != null) {
            cache.computeIfPresent(key, (k, existing) ->
                    existing.response() != null && diagnosisId.equals(existing.response().diagnosisId())
                            ? null
                            : existing
            );
        }
    }

    @Override
    public void evictById(UUID diagnosisId) {
        evictByDiagnosisId(diagnosisId);
    }

    /**
     * Clears all cached diagnosis entries and reverse indices, intended for test isolation.
     */
    public void clear() {
        cache.clear();
        diagnosisIdToKey.clear();
    }

    private String buildKey(UUID userId, String imageHash) {
        return userId + ":" + imageHash;
    }

    private void removeReverseIndex(CacheEntry entry, String key) {
        if (entry != null && entry.response() != null && entry.response().diagnosisId() != null) {
            diagnosisIdToKey.remove(entry.response().diagnosisId(), key);
        }
    }

    private void cleanExpiredEntries() {
        Instant now = Instant.now(clock);
        for (Map.Entry<String, CacheEntry> mapEntry : cache.entrySet()) {
            String key = mapEntry.getKey();
            CacheEntry entry = mapEntry.getValue();
            if (entry.isExpired(now) && cache.remove(key, entry)) {
                removeReverseIndex(entry, key);
            }
        }
    }
}
