package com.eiu.pdds.treatment.service.impl;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.dto.treatment.TreatmentCreateRequest;
import com.eiu.pdds.dto.treatment.TreatmentDto;
import com.eiu.pdds.dto.treatment.TreatmentUpdateRequest;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.plant.repository.PlantRepository;
import com.eiu.pdds.treatment.entity.Treatment;
import com.eiu.pdds.treatment.entity.TreatmentType;
import com.eiu.pdds.treatment.repository.TreatmentRepository;
import com.eiu.pdds.treatment.service.TreatmentManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Production implementation of TreatmentManagementService (SRP, DIP).
 * Handles transactional business operations for therapeutic and mitigation protocols.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TreatmentManagementServiceImpl implements TreatmentManagementService {

    private final TreatmentRepository treatmentRepository;
    private final DiseaseRepository diseaseRepository;
    private final PlantRepository plantRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentDto> getTreatmentsByDiseaseId(UUID diseaseId, TreatmentType treatmentType, UUID plantId) {
        if (!diseaseRepository.existsById(diseaseId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Disease not found with ID: " + diseaseId);
        }

        // Verify disease is not soft-deleted
        diseaseRepository.findByIdAndDeletedAtIsNull(diseaseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Disease not found with ID: " + diseaseId));

        List<Treatment> treatments;
        if (treatmentType != null && plantId != null) {
            treatments = treatmentRepository.findByDiseaseIdAndPlantIdAndTreatmentTypeAndDeletedAtIsNull(diseaseId, plantId, treatmentType);
        } else if (treatmentType != null) {
            treatments = treatmentRepository.findByDiseaseIdAndTreatmentTypeAndDeletedAtIsNull(diseaseId, treatmentType);
        } else if (plantId != null) {
            treatments = treatmentRepository.findByDiseaseIdAndPlantIdAndDeletedAtIsNull(diseaseId, plantId);
        } else {
            treatments = treatmentRepository.findByDiseaseIdAndDeletedAtIsNull(diseaseId);
        }

        return treatments.stream()
                .map(this::toTreatmentDto)
                .toList();
    }

    @Override
    @Transactional
    public TreatmentDto createTreatment(UUID diseaseId, TreatmentCreateRequest request) {
        Disease disease = diseaseRepository.findByIdAndDeletedAtIsNull(diseaseId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Disease not found with ID: " + diseaseId));

        Plant plant = null;
        if (request.plantId() != null) {
            plant = plantRepository.findById(request.plantId())
                    .filter(p -> !p.isDeleted())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Plant crop not found with ID: " + request.plantId()));
        }

        Treatment treatment = Treatment.builder()
                .disease(disease)
                .plant(plant)
                .treatmentType(request.treatmentType())
                .title(request.title().trim())
                .instructions(request.instructions().trim())
                .activeIngredients(request.activeIngredients() != null ? request.activeIngredients().trim() : null)
                .dosageRecommendation(request.dosageRecommendation() != null ? request.dosageRecommendation().trim() : null)
                .safetyIntervalDays(request.safetyIntervalDays())
                .warning(request.warning() != null ? request.warning().trim() : null)
                .build();

        Treatment saved = treatmentRepository.save(treatment);
        log.info("Created treatment protocol: ID={}, diseaseId={}, type={}", saved.getId(), diseaseId, saved.getTreatmentType());

        return toTreatmentDto(saved);
    }

    @Override
    @Transactional
    public TreatmentDto updateTreatment(UUID treatmentId, TreatmentUpdateRequest request) {
        Treatment treatment = treatmentRepository.findByIdAndDeletedAtIsNull(treatmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Treatment not found with ID: " + treatmentId));

        if (request.plantId() != null) {
            Plant plant = plantRepository.findById(request.plantId())
                    .filter(p -> !p.isDeleted())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Plant crop not found with ID: " + request.plantId()));
            treatment.setPlant(plant);
        } else {
            treatment.setPlant(null);
        }

        treatment.setTreatmentType(request.treatmentType());
        treatment.setTitle(request.title().trim());
        treatment.setInstructions(request.instructions().trim());
        treatment.setActiveIngredients(request.activeIngredients() != null ? request.activeIngredients().trim() : null);
        treatment.setDosageRecommendation(request.dosageRecommendation() != null ? request.dosageRecommendation().trim() : null);
        treatment.setSafetyIntervalDays(request.safetyIntervalDays());
        treatment.setWarning(request.warning() != null ? request.warning().trim() : null);

        Treatment saved = treatmentRepository.save(treatment);
        log.info("Updated treatment protocol: ID={}, title={}", saved.getId(), saved.getTitle());

        return toTreatmentDto(saved);
    }

    @Override
    @Transactional
    public void deleteTreatment(UUID treatmentId) {
        Treatment treatment = treatmentRepository.findByIdAndDeletedAtIsNull(treatmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Treatment not found with ID: " + treatmentId));

        treatment.markDeleted();
        treatmentRepository.save(treatment);
        log.info("Soft-deleted treatment protocol: ID={}", treatment.getId());
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
