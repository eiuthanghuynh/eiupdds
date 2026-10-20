package com.eiu.pdds.dto.treatment;

import com.eiu.pdds.treatment.entity.TreatmentType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Agricultural treatment protocol DTO describing remedies, instructions, dosages, and safety intervals.
 */
@Schema(description = "Agricultural treatment and mitigation protocol")
public record TreatmentDto(
        @Schema(description = "Unique identifier of the treatment", example = "44444444-4444-4444-4444-444444444401")
        UUID id,

        @Schema(description = "Category of treatment: BIOLOGICAL, CHEMICAL, PREVENTIVE, CULTURAL", example = "CHEMICAL")
        TreatmentType treatmentType,

        @Schema(description = "Protocol title", example = "Metalaxyl + Mancozeb Systemic Fungicide Spray")
        String title,

        @Schema(description = "Actionable application instructions", example = "Spray thoroughly on both leaf surfaces upon first appearance.")
        String instructions,

        @Schema(description = "Chemical active ingredients or biological strains", example = "Metalaxyl 8% + Mancozeb 64%")
        String activeIngredients,

        @Schema(description = "Recommended dosage and mixture ratio", example = "50g per 16L sprayer (2.5 - 3.0 kg/ha)")
        String dosageRecommendation,

        @Schema(description = "Safety harvest interval in days after application", example = "7")
        Integer safetyIntervalDays,

        @Schema(description = "Critical precautions or chemical resistance warning", example = "Do not apply more than 3 consecutive sprays per season.")
        String warning
) {}
