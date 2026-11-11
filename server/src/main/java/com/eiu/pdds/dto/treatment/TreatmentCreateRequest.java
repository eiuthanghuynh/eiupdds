package com.eiu.pdds.dto.treatment;

import com.eiu.pdds.treatment.entity.TreatmentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request payload to create a new agricultural treatment protocol for a plant disease.
 */
@Schema(description = "Request payload to create a new treatment protocol for a plant disease")
public record TreatmentCreateRequest(
        @Schema(description = "Category of treatment: BIOLOGICAL, CHEMICAL, PREVENTIVE, CULTURAL", example = "CHEMICAL")
        @NotNull(message = "Treatment type is required")
        TreatmentType treatmentType,

        @Schema(description = "Optional plant ID if treatment is specific to a host plant", example = "11111111-1111-1111-1111-111111111101")
        UUID plantId,

        @Schema(description = "Treatment title", example = "Metalaxyl + Mancozeb Spray")
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @Schema(description = "Actionable application instructions", example = "Apply at first sign of disease symptoms.")
        @NotBlank(message = "Instructions are required")
        String instructions,

        @Schema(description = "Chemical active ingredients or biological strains", example = "Metalaxyl 8% + Mancozeb 64%")
        String activeIngredients,

        @Schema(description = "Recommended dosage and mixture ratio", example = "50g per 16L water sprayer")
        String dosageRecommendation,

        @Schema(description = "Safety harvest interval in days after application", example = "7")
        @PositiveOrZero(message = "Safety interval days must be non-negative")
        Integer safetyIntervalDays,

        @Schema(description = "Precautionary statements or chemical resistance warnings", example = "Do not apply more than 3 consecutive sprays per season.")
        String warning
) {}
