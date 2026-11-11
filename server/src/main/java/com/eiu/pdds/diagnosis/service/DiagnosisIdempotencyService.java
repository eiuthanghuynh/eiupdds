package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Service contract for request idempotency and AI quota protection (Enterprise Architecture).
 * Guards against redundant AI inference calls when identical leaf images are sent concurrently or retried.
 */
public interface DiagnosisIdempotencyService {

    /**
     * Looks up recent diagnosis result by image hash for an optional user.
     *
     * @param userId    authenticated user ID (null for anonymous)
     * @param imageHash SHA-256 hex string of the image bytes
     * @return Optional containing cached diagnosis response if present and valid
     */
    Optional<DiagnosisAnalyzeResponse> findRecentDiagnosis(UUID userId, String imageHash);

    default Optional<DiagnosisAnalyzeResponse> findRecentDiagnosis(String imageHash) {
        return findRecentDiagnosis(null, imageHash);
    }

    /**
     * Records completed diagnosis in the idempotency cache for an optional user.
     *
     * @param userId    authenticated user ID (null for anonymous)
     * @param imageHash SHA-256 hex string of the image bytes
     * @param response  the diagnosis response to cache
     */
    void recordDiagnosis(UUID userId, String imageHash, DiagnosisAnalyzeResponse response);

    default void recordDiagnosis(String imageHash, DiagnosisAnalyzeResponse response) {
        recordDiagnosis(null, imageHash, response);
    }

    /**
     * Evicts entry from cache.
     *
     * @param userId    authenticated user ID (null for anonymous)
     * @param imageHash SHA-256 hex string of the image bytes
     */
    void evict(UUID userId, String imageHash);

    default void evict(String imageHash) {
        evict(null, imageHash);
    }

    /**
     * Evicts cached diagnosis entry by diagnosis ID.
     *
     * @param diagnosisId unique diagnosis identifier to evict
     */
    void evictByDiagnosisId(UUID diagnosisId);
}
