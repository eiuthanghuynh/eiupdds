package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeRequest;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Service contract for plant leaf disease analysis, Computer Vision inference,
 * dual-coordinate transformation, pathology resolution, and agricultural treatment recommendations.
 */
public interface DiagnosisService {

    /**
     * Executes diagnosis pipeline from an encapsulated parameter request object (Clean Code).
     */
    DiagnosisAnalyzeResponse analyze(DiagnosisAnalyzeRequest request);

    /**
     * Executes the end-to-end diagnosis pipeline on an uploaded plant leaf image.
     */
    default DiagnosisAnalyzeResponse analyzeImage(
            MultipartFile image,
            String plantCodeHint,
            Double latitude,
            Double longitude,
            String deviceInfo
    ) {
        return analyzeImage(image, plantCodeHint, latitude, longitude, deviceInfo, null);
    }

    /**
     * Executes the end-to-end diagnosis pipeline on an uploaded plant leaf image with authenticated user context.
     *
     * @param image         the leaf image file (JPEG, PNG, WebP <= 15MB)
     * @param plantCodeHint optional user/client hint for the plant crop (e.g. "POTATO", "TOMATO")
     * @param latitude      optional GPS latitude coordinate where image was captured
     * @param longitude     optional GPS longitude coordinate where image was captured
     * @param deviceInfo    optional client device identification string
     * @param userId        optional authenticated user UUID
     * @return standardized diagnosis response containing dominant pathology, normalized bounding boxes, and treatments
     */
    DiagnosisAnalyzeResponse analyzeImage(
            MultipartFile image,
            String plantCodeHint,
            Double latitude,
            Double longitude,
            String deviceInfo,
            UUID userId
    );

    /**
     * Soft-deletes a diagnosis session belonging to the authenticated user.
     * Enforces strict data ownership and evicts from idempotency cache.
     *
     * @param userId      the authenticated user identifier
     * @param diagnosisId the diagnosis session UUID
     */
    void softDeleteUserDiagnosis(UUID userId, UUID diagnosisId);
}
