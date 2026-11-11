package com.eiu.pdds.treatment.service.impl;

import com.eiu.pdds.dto.treatment.TreatmentDto;
import com.eiu.pdds.treatment.entity.Treatment;
import com.eiu.pdds.treatment.entity.TreatmentType;
import com.eiu.pdds.treatment.repository.TreatmentRepository;
import com.eiu.pdds.treatment.service.TreatmentRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Production implementation of TreatmentRecommendationService (SRP).
 */
@Service
@RequiredArgsConstructor
public class TreatmentRecommendationServiceImpl implements TreatmentRecommendationService {

    private static final UUID HEALTHY_CARE_TREATMENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444499");

    private final TreatmentRepository treatmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentDto> getTreatmentsForDisease(UUID diseaseId) {
        if (diseaseId == null) {
            return Collections.emptyList();
        }
        return treatmentRepository.findByDiseaseIdAndDeletedAtIsNull(diseaseId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<TreatmentDto> getHealthyCareTreatments(String plantCode) {
        TreatmentDto healthyCare = new TreatmentDto(
                HEALTHY_CARE_TREATMENT_ID,
                TreatmentType.PREVENTIVE,
                "Routine Crop Care & Foliage Monitoring",
                "Maintain adequate planting distance, provide balanced soil nutrition, avoid overhead sprinkler splash, and routinely inspect foliage for early sign of fungal or bacterial sporulation.",
                "None (Natural Cultural Practice)",
                "Standard canopy aeration and drip irrigation at root zone",
                0,
                "Continue routine preventive monitoring during humid morning hours."
        );
        return List.of(healthyCare);
    }

    private TreatmentDto toDto(Treatment t) {
        return new TreatmentDto(
                t.getId(),
                t.getTreatmentType(),
                t.getTitle(),
                t.getInstructions(),
                t.getActiveIngredients(),
                t.getDosageRecommendation(),
                t.getSafetyIntervalDays(),
                t.getWarning()
        );
    }
}
