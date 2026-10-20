package com.eiu.pdds.dto.disease;

import com.eiu.pdds.disease.entity.Severity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Summary DTO for disease information.
 */
@Schema(description = "Summary information of a plant pathology")
public record DiseaseSummaryDto(
        @Schema(description = "Unique identifier of the disease", example = "22222222-2222-2222-2222-222222222201")
        UUID id,

        @Schema(description = "Unique disease code", example = "POTATO_LATE_BLIGHT")
        String diseaseCode,

        @Schema(description = "Common disease name", example = "Potato leaf late blight (Bệnh mốc sương khoai tây)")
        String commonName,

        @Schema(description = "Scientific binomial nomenclature", example = "Phytophthora infestans")
        String scientificName,

        @Schema(description = "Pathological severity level (LOW, MODERATE, HIGH, CRITICAL)", example = "CRITICAL")
        Severity severity
) {}
