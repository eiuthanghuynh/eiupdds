package com.eiu.pdds.dto.diagnosis;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * Parameter Object encapsulating multipart leaf analysis request parameters (Clean Code).
 */
@Schema(description = "Multipart ingestion request payload for plant leaf image analysis")
public record DiagnosisAnalyzeRequest(
        @Schema(description = "Leaf photo file (JPEG, PNG, WebP; max 15MB)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Image file is mandatory")
        MultipartFile image,

        @Schema(description = "Optional crop hint code (e.g., POTATO, TOMATO, RICE)", example = "POTATO")
        String plantCode,

        @Schema(description = "GPS Latitude coordinate", example = "11.0528")
        Double latitude,

        @Schema(description = "GPS Longitude coordinate", example = "106.6661")
        Double longitude,

        @Schema(description = "Client hardware or OS info", example = "iPhone 15 Pro / iOS 17.5")
        String deviceInfo
) {}
