package com.eiu.pdds.diagnosis.service.dto;

import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import com.eiu.pdds.plant.entity.Plant;

import java.util.List;

/**
 * Immutable domain result of leaf pathology classification and analysis (SRP).
 */
public record PathologyResolution(
        Disease dominantDiseaseEntity,
        DiseaseSummaryDto dominantDiseaseDto,
        Double dominantConfidence,
        boolean isHealthy,
        List<DiseaseSummaryDto> coOccurringDiseases,
        Plant plantEntity
) {
    public static PathologyResolution empty(Plant plant) {
        return new PathologyResolution(null, null, null, false, List.of(), plant);
    }
}
