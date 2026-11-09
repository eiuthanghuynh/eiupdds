package com.eiu.pdds.disease.service.impl;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.PlantDisease;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.disease.service.DiseaseManagementService;
import com.eiu.pdds.dto.disease.DiseaseCreateRequest;
import com.eiu.pdds.dto.disease.DiseaseDetailDto;
import com.eiu.pdds.dto.disease.DiseaseUpdateRequest;
import com.eiu.pdds.dto.treatment.TreatmentDto;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.plant.repository.PlantRepository;
import com.eiu.pdds.treatment.entity.Treatment;
import com.eiu.pdds.treatment.repository.TreatmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Production implementation of DiseaseManagementService (CQRS Command Model, SRP, DIP).
 * Handles transactional business operations for disease lifecycle, plant associations, and soft deletion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiseaseManagementServiceImpl implements DiseaseManagementService {

    private final DiseaseRepository diseaseRepository;
    private final PlantRepository plantRepository;
    private final TreatmentRepository treatmentRepository;

    @Override
    @Transactional
    public DiseaseDetailDto createDisease(DiseaseCreateRequest request) {
        String normalizedCode = request.diseaseCode().trim().toUpperCase();

        if (diseaseRepository.existsByDiseaseCodeAndDeletedAtIsNull(normalizedCode)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Disease code already exists: " + request.diseaseCode());
        }

        Disease disease = Disease.builder()
                .diseaseCode(normalizedCode)
                .commonName(request.commonName().trim())
                .scientificName(request.scientificName() != null ? request.scientificName().trim() : null)
                .severity(request.severity())
                .symptoms(request.symptoms())
                .description(request.description())
                .plantDiseases(new HashSet<>())
                .treatments(Collections.emptyList())
                .build();

        if (request.plantCodes() != null && !request.plantCodes().isEmpty()) {
            for (String plantCode : request.plantCodes()) {
                if (plantCode == null || plantCode.isBlank()) {
                    continue;
                }
                String code = plantCode.trim().toUpperCase();
                Plant plant = plantRepository.findByPlantCode(code)
                        .filter(p -> !p.isDeleted())
                        .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Plant crop not found with code: " + code));

                PlantDisease plantDisease = PlantDisease.builder()
                        .plant(plant)
                        .disease(disease)
                        .build();
                disease.getPlantDiseases().add(plantDisease);
            }
        }

        Disease saved = diseaseRepository.save(disease);
        log.info("Created new disease catalog entry: ID={}, code={}", saved.getId(), saved.getDiseaseCode());

        return toDetailDto(saved, Collections.emptyList());
    }

    @Override
    @Transactional
    public DiseaseDetailDto updateDisease(UUID id, DiseaseUpdateRequest request) {
        Disease disease = diseaseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Disease not found with ID: " + id));

        disease.setCommonName(request.commonName().trim());
        disease.setScientificName(request.scientificName() != null ? request.scientificName().trim() : null);
        disease.setSeverity(request.severity());
        disease.setSymptoms(request.symptoms());
        disease.setDescription(request.description());

        if (request.plantCodes() != null) {
            if (disease.getPlantDiseases() == null) {
                disease.setPlantDiseases(new HashSet<>());
            }

            Set<String> targetCodes = new HashSet<>();
            Map<String, Plant> resolvedPlants = new HashMap<>();

            for (String code : request.plantCodes()) {
                if (code == null || code.isBlank()) {
                    continue;
                }
                String normalized = code.trim().toUpperCase();
                targetCodes.add(normalized);
                if (!resolvedPlants.containsKey(normalized)) {
                    Plant plant = plantRepository.findByPlantCode(normalized)
                            .filter(p -> !p.isDeleted())
                            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Plant crop not found with code: " + code));
                    resolvedPlants.put(normalized, plant);
                }
            }

            // Remove disassociated plants
            disease.getPlantDiseases().removeIf(pd -> !targetCodes.contains(pd.getPlant().getPlantCode().toUpperCase()));

            // Add new associations
            Set<String> currentCodes = disease.getPlantDiseases().stream()
                    .map(pd -> pd.getPlant().getPlantCode().toUpperCase())
                    .collect(Collectors.toSet());

            for (Map.Entry<String, Plant> entry : resolvedPlants.entrySet()) {
                if (!currentCodes.contains(entry.getKey())) {
                    PlantDisease plantDisease = PlantDisease.builder()
                            .plant(entry.getValue())
                            .disease(disease)
                            .build();
                    disease.getPlantDiseases().add(plantDisease);
                }
            }
        }

        Disease saved = diseaseRepository.save(disease);
        log.info("Updated disease catalog entry: ID={}, code={}", saved.getId(), saved.getDiseaseCode());

        List<TreatmentDto> treatments = treatmentRepository.findByDiseaseIdAndDeletedAtIsNull(saved.getId()).stream()
                .map(this::toTreatmentDto)
                .toList();

        return toDetailDto(saved, treatments);
    }

    @Override
    @Transactional
    public void deleteDisease(UUID id) {
        Disease disease = diseaseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Disease not found with ID: " + id));

        disease.markDeleted();
        diseaseRepository.save(disease);
        log.info("Soft-deleted disease catalog entry: ID={}, code={}", disease.getId(), disease.getDiseaseCode());
    }

    private DiseaseDetailDto toDetailDto(Disease disease, List<TreatmentDto> treatments) {
        return new DiseaseDetailDto(
                disease.getId(),
                disease.getDiseaseCode(),
                disease.getCommonName(),
                disease.getScientificName(),
                disease.getSeverity(),
                disease.getSymptoms(),
                disease.getDescription(),
                treatments
        );
    }

    private TreatmentDto toTreatmentDto(Treatment treatment) {
        return new TreatmentDto(
                treatment.getId(),
                treatment.getTreatmentType(),
                treatment.getTitle(),
                treatment.getInstructions(),
                treatment.getActiveIngredients(),
                treatment.getDosageRecommendation(),
                treatment.getSafetyIntervalDays(),
                treatment.getWarning()
        );
    }
}
