package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;

import java.util.Optional;

/**
 * Service contract for request idempotency and AI quota protection (Enterprise Architecture).
 * Guards against redundant AI inference calls when identical leaf images are sent concurrently or retried.
 */
public interface DiagnosisIdempotencyService {

    /**
     * Looks up recent diagnosis result by image hash.
     *
     * @param imageHash SHA-256 hex string of the image bytes
     * @return Optional containing cached diagnosis response if present and valid
     */
    Optional<DiagnosisAnalyzeResponse> findRecentDiagnosis(String imageHash);

    /**
     * Records completed diagnosis in the idempotency cache.
     *
     * @param imageHash SHA-256 hex string of the image bytes
     * @param response  the diagnosis response to cache
     */
    void recordDiagnosis(String imageHash, DiagnosisAnalyzeResponse response);

    /**
     * Evicts entry from cache.
     *
     * @param imageHash SHA-256 hex string of the image bytes
     */
    void evict(String imageHash);
}
