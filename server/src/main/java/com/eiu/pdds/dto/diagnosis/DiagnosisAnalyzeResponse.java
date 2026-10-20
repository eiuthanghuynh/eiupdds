package com.eiu.pdds.dto.diagnosis;

import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import com.eiu.pdds.dto.treatment.TreatmentDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Main response contract for leaf disease diagnosis analysis.
 * Provides dominant disease identification, normalized bounding box coordinates for mobile SVG rendering,
 * and immediate actionable treatment protocols.
 */
@Schema(description = "Inference outcome and treatment recommendations from leaf image analysis")
public record DiagnosisAnalyzeResponse(
        @Schema(description = "Unique ID of the diagnosis session", example = "55555555-5555-5555-5555-555555555501")
        UUID diagnosisId,

        @Schema(description = "Storage URL of the original analyzed leaf image", example = "https://s3.eiu.edu.vn/eiu-pdds-plants/potato-leaf-sample.jpg")
        String originalImageUrl,

        @Schema(description = "Original image pixel width", example = "399")
        Integer imageWidth,

        @Schema(description = "Original image pixel height", example = "501")
        Integer imageHeight,

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

        @Schema(description = "Diagnosis processing status (COMPLETED, PENDING, FAILED)", example = "COMPLETED")
        String status,

        @Schema(description = "Timestamp when diagnosis was generated")
        Instant diagnosedAt
) {}
