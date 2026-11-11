package com.eiu.pdds.dto.disease;

import com.eiu.pdds.disease.entity.Severity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request payload to create a new disease catalog entry.
 */
@Schema(description = "Request payload to create a new disease catalog entry")
public record DiseaseCreateRequest(
        @Schema(description = "Unique disease code in uppercase alphanumeric with underscores", example = "POTATO_LATE_BLIGHT")
        @NotBlank(message = "Disease code is required")
        @Pattern(regexp = "^[A-Z0-9_]{3,64}$", message = "Disease code must be uppercase alphanumeric with underscores")
        String diseaseCode,

        @Schema(description = "Common name of the disease", example = "Potato Late Blight")
        @NotBlank(message = "Common name is required")
        @Size(max = 255, message = "Common name must not exceed 255 characters")
        String commonName,

        @Schema(description = "Scientific binomial nomenclature", example = "Phytophthora infestans")
        @Size(max = 255, message = "Scientific name must not exceed 255 characters")
        String scientificName,

        @Schema(description = "Pathological severity level", example = "CRITICAL")
        @NotNull(message = "Severity is required")
        Severity severity,

        @Schema(description = "Typical symptoms on foliage, stems, or fruits")
        String symptoms,

        @Schema(description = "Detailed pathological description, etiology, and life cycle")
        String description,

        @Schema(description = "List of associated plant crop codes", example = "[\"POTATO\", \"TOMATO\"]")
        List<String> plantCodes
) {}
