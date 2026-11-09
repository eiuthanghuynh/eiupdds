package com.eiu.pdds.dto.diagnosis;

import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import com.eiu.pdds.dto.plant.PlantDto;
import com.eiu.pdds.dto.treatment.TreatmentDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Main response contract for leaf disease diagnosis analysis.
 * Provides dominant disease identification, host crop details, normalized bounding box
 * coordinates for mobile SVG rendering, geolocation telemetry, and actionable treatment protocols.
 */
@Schema(description = "Inference outcome, host plant metadata, and treatment recommendations from leaf image analysis")
public record DiagnosisAnalyzeResponse(
        @Schema(description = "Unique ID of the diagnosis session", example = "55555555-5555-5555-5555-555555555501")
        UUID diagnosisId,

        @Schema(description = "Storage URL of the original analyzed leaf image", example = "https://s3.eiu.edu.vn/eiu-pdds-plants/potato-leaf-sample.jpg")
        String originalImageUrl,

        @Schema(description = "Original image pixel width", example = "399")
        Integer imageWidth,

        @Schema(description = "Original image pixel height", example = "501")
        Integer imageHeight,

        @Schema(description = "Host plant identified or matched for the diagnosis")
        PlantDto plant,

        @Schema(description = "Primary pathology identified by highest confidence detection")
        DiseaseSummaryDto dominantDisease,

        @Schema(description = "Confidence score of the dominant diagnosis between 0.0 and 1.0", example = "0.7994")
        Double dominantConfidence,

        @Schema(description = "Detected leaf bounding boxes with dual coordinate systems (pixel & normalized)")
        List<BoundingBoxDto> boundingBoxes,

        @Schema(description = "Other distinct pathologies co-occurring in the image (co-infection)")
        List<DiseaseSummaryDto> coOccurringDiseases,

        @Schema(description = "Curated biological, chemical, and cultural treatment protocols for the dominant pathology")
        List<TreatmentDto> recommendedTreatments,

        @Schema(description = "Roboflow Computer Vision inference latency in milliseconds", example = "285")
        Integer inferenceTimeMs,

        @Schema(description = "GPS latitude where the image was captured", example = "11.0528")
        Double latitude,

        @Schema(description = "GPS longitude where the image was captured", example = "106.6661")
        Double longitude,

        @Schema(description = "Client device info string", example = "iPhone 15 Pro / iOS 17.5")
        String deviceInfo,

        @Schema(description = "Diagnosis processing status (COMPLETED, PENDING, FAILED)", example = "COMPLETED")
        String status,

        @Schema(description = "Timestamp when diagnosis was generated")
        Instant diagnosedAt
) {
    /**
     * Backward-compatible 12-argument constructor for legacy callers and tests.
     */
    public DiagnosisAnalyzeResponse(
            UUID diagnosisId,
            String originalImageUrl,
            Integer imageWidth,
            Integer imageHeight,
            DiseaseSummaryDto dominantDisease,
            Double dominantConfidence,
            List<BoundingBoxDto> boundingBoxes,
            List<DiseaseSummaryDto> coOccurringDiseases,
            List<TreatmentDto> recommendedTreatments,
            Integer inferenceTimeMs,
            String status,
            Instant diagnosedAt
    ) {
        this(
                diagnosisId,
                originalImageUrl,
                imageWidth,
                imageHeight,
                null,
                dominantDisease,
                dominantConfidence,
                boundingBoxes,
                coOccurringDiseases,
                recommendedTreatments,
                inferenceTimeMs,
                null,
                null,
                null,
                status,
                diagnosedAt
        );
    }
}
