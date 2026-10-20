package com.eiu.pdds.dto.disease;

import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.dto.treatment.TreatmentDto;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Detailed disease catalog DTO containing symptoms, comprehensive description, and mitigation treatments.
 */
@Schema(description = "Detailed plant disease catalog record including treatments")
public record DiseaseDetailDto(
        @Schema(description = "Unique identifier of the disease", example = "22222222-2222-2222-2222-222222222201")
        UUID id,

        @Schema(description = "Unique disease code", example = "POTATO_LATE_BLIGHT")
        String diseaseCode,

        @Schema(description = "Common disease name", example = "Potato leaf late blight (Bệnh mốc sương khoai tây)")
        String commonName,

        @Schema(description = "Scientific binomial nomenclature", example = "Phytophthora infestans")
        String scientificName,

        @Schema(description = "Pathological severity level (LOW, MODERATE, HIGH, CRITICAL)", example = "CRITICAL")
        Severity severity,

        @Schema(description = "Typical morphological symptoms on foliage, stems, or fruit")
        String symptoms,

        @Schema(description = "Pathological background, life cycle, and favorable conditions")
        String description,

        @Schema(description = "List of validated biological, chemical, and cultural treatments")
        List<TreatmentDto> treatments
) {}
