package com.eiu.pdds.dto.diagnosis;

import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import com.eiu.pdds.dto.plant.PlantDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Lightweight summary of a historical plant diagnosis session")
public record DiagnosisSummaryDto(
        @Schema(description = "Unique diagnosis session identifier")
        UUID diagnosisId,

        @Schema(description = "URL of the uploaded leaf image in object storage")
        String originalImageUrl,

        @Schema(description = "Plant crop information")
        PlantDto plant,

        @Schema(description = "Dominant pathology classification summary")
        DiseaseSummaryDto dominantDisease,

        @Schema(description = "Confidence score of the dominant diagnosis [0.0 - 1.0]", example = "0.7994")
        Double dominantConfidence,

        @Schema(description = "Total number of bounding box detections detected in the image", example = "5")
        int boundingBoxCount,

        @Schema(description = "GPS latitude", example = "11.0528")
        Double latitude,

        @Schema(description = "GPS longitude", example = "106.6661")
        Double longitude,

        @Schema(description = "Client device info", example = "iPhone 15 Pro / iOS 17.5")
        String deviceInfo,

        @Schema(description = "Diagnosis status", example = "COMPLETED")
        String status,

        @Schema(description = "Timestamp when the diagnosis was performed")
        Instant createdAt
) {
}
