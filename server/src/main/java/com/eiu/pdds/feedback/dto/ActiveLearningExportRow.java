package com.eiu.pdds.feedback.dto;

import java.time.Instant;
import java.util.UUID;

public record ActiveLearningExportRow(
        UUID feedbackId,
        UUID diagnosisId,
        String originalImageUrl,
        String plantCode,
        String predictedDiseaseCode,
        String verifiedDiseaseCode,
        Boolean isAccurate,
        String boundingBoxesJson,
        String expertNotes,
        Instant verifiedAt
) {
}
