package com.eiu.pdds.feedback.dto;

import java.time.Instant;
import java.util.UUID;

public record FeedbackResponseDto(
        UUID id,
        UUID diagnosisId,
        String diagnosisImageUrl,
        String plantCode,
        String plantCommonName,
        String predictedDiseaseCode,
        String predictedDiseaseName,
        Double predictedConfidence,
        UUID submittedByUserId,
        String submittedByUsername,
        Boolean isAccurate,
        UUID suggestedDiseaseId,
        String suggestedDiseaseCode,
        String suggestedDiseaseName,
        String userComment,
        Boolean expertVerified,
        UUID verifiedByUserId,
        String verifiedByUsername,
        Instant verifiedAt,
        String expertNotes,
        Instant createdAt,
        Instant updatedAt
) {
}
