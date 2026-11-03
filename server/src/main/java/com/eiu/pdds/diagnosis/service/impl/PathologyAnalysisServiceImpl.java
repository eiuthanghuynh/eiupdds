package com.eiu.pdds.diagnosis.service.impl;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.diagnosis.service.PathologyAnalysisService;
import com.eiu.pdds.diagnosis.service.dto.PathologyResolution;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import com.eiu.pdds.integration.ai.dto.DetectedObject;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.plant.repository.PlantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.eiu.pdds.diagnosis.service.CoordinateTransformer.round;

/**
 * Production implementation of PathologyAnalysisService (SRP).
 * Vendor-agnostic domain service operating on normalized domain entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PathologyAnalysisServiceImpl implements PathologyAnalysisService {

    private final DiseaseRepository diseaseRepository;
    private final PlantRepository plantRepository;

    @Override
    public PathologyResolution resolvePathology(List<DetectedObject> detections, String plantCodeHint) {
        if (detections == null || detections.isEmpty()) {
            Plant plant = resolvePlant(plantCodeHint, null);
            return PathologyResolution.empty(plant);
        }

        // 1. Dominant Pathology Resolution (Max Confidence detection)
        DetectedObject dominantPrediction = detections.stream()
                .max(Comparator.comparingDouble(DetectedObject::confidence))
                .orElse(detections.get(0));

        String dominantDiseaseCode = dominantPrediction.diseaseCode();
        double dominantConfidence = round(dominantPrediction.confidence(), 4);
        boolean isHealthy = dominantPrediction.isHealthy();

        Disease dominantDiseaseEntity = null;
        DiseaseSummaryDto dominantDiseaseDto;

        if (isHealthy) {
            log.info("Dominant detection is Healthy Foliage: code='{}', label='{}'",
                    dominantDiseaseCode, dominantPrediction.label());
            dominantDiseaseDto = new DiseaseSummaryDto(
                    null,
                    dominantDiseaseCode,
                    dominantPrediction.englishName() != null ? dominantPrediction.englishName() : dominantPrediction.label(),
                    "Healthy foliage (No pathology detected)",
                    Severity.LOW
            );
        } else {
            dominantDiseaseEntity = diseaseRepository.findByDiseaseCode(dominantDiseaseCode)
                    .orElseThrow(() -> new BusinessException(ErrorCode.DISEASE_NOT_FOUND,
                            "Pathology not registered in catalog: " + dominantDiseaseCode));

            dominantDiseaseDto = new DiseaseSummaryDto(
                    dominantDiseaseEntity.getId(),
                    dominantDiseaseEntity.getDiseaseCode(),
                    dominantDiseaseEntity.getCommonName(),
                    dominantDiseaseEntity.getScientificName(),
                    dominantDiseaseEntity.getSeverity()
            );
        }

        // 2. Co-occurring Pathologies (Distinct secondary pathologies)
        List<DiseaseSummaryDto> coOccurringDiseases = detections.stream()
                .filter(d -> d.diseaseCode() != null && !d.diseaseCode().equalsIgnoreCase(dominantDiseaseCode))
                .map(d -> diseaseRepository.findByDiseaseCode(d.diseaseCode())
                        .map(entity -> new DiseaseSummaryDto(
                                entity.getId(),
                                entity.getDiseaseCode(),
                                entity.getCommonName(),
                                entity.getScientificName(),
                                entity.getSeverity()))
                        .orElseGet(() -> new DiseaseSummaryDto(
                                null,
                                d.diseaseCode(),
                                d.englishName() != null ? d.englishName() : d.label(),
                                "Co-occurring pathology",
                                Severity.MODERATE)))
                .distinct()
                .toList();

        // 3. Resolve Plant Crop Entity
        Plant plantEntity = resolvePlant(plantCodeHint, dominantPrediction.plantCode());

        return new PathologyResolution(
                dominantDiseaseEntity,
                dominantDiseaseDto,
                dominantConfidence,
                isHealthy,
                coOccurringDiseases,
                plantEntity
        );
    }

    private Plant resolvePlant(String plantCodeHint, String detectedPlantCode) {
        if (plantCodeHint != null && !plantCodeHint.isBlank()) {
            Optional<Plant> plantOpt = plantRepository.findByPlantCode(plantCodeHint.toUpperCase().trim());
            if (plantOpt.isPresent()) {
                return plantOpt.get();
            }
        }
        if (detectedPlantCode != null && !detectedPlantCode.isBlank()) {
            return plantRepository.findByPlantCode(detectedPlantCode.toUpperCase().trim()).orElse(null);
        }
        return null;
    }
}
