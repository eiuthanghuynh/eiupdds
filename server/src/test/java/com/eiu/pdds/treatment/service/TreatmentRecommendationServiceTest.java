package com.eiu.pdds.treatment.service;

import com.eiu.pdds.dto.treatment.TreatmentDto;
import com.eiu.pdds.treatment.entity.Treatment;
import com.eiu.pdds.treatment.entity.TreatmentType;
import com.eiu.pdds.treatment.repository.TreatmentRepository;
import com.eiu.pdds.treatment.service.impl.TreatmentRecommendationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TreatmentRecommendationServiceTest {

    @Mock
    private TreatmentRepository treatmentRepository;

    private TreatmentRecommendationService treatmentService;

    @BeforeEach
    void setUp() {
        treatmentService = new TreatmentRecommendationServiceImpl(treatmentRepository);
    }

    @Test
    @DisplayName("Should return mapped treatment DTOs for a registered pathology")
    void testGetTreatmentsForDisease_Success() {
        UUID diseaseId = UUID.randomUUID();
        Treatment treatment = Treatment.builder()
                .treatmentType(TreatmentType.BIOLOGICAL)
                .title("Trichoderma Spray")
                .instructions("Apply bio-fungicide weekly")
                .activeIngredients("Trichoderma harzianum")
                .dosageRecommendation("2g per liter")
                .safetyIntervalDays(0)
                .warning("Do not mix with chemical copper")
                .build();

        when(treatmentRepository.findByDiseaseIdAndDeletedAtIsNull(diseaseId)).thenReturn(List.of(treatment));

        List<TreatmentDto> result = treatmentService.getTreatmentsForDisease(diseaseId);

        assertThat(result).hasSize(1);
        TreatmentDto dto = result.get(0);
        assertThat(dto.treatmentType()).isEqualTo(TreatmentType.BIOLOGICAL);
        assertThat(dto.title()).isEqualTo("Trichoderma Spray");
        assertThat(dto.activeIngredients()).isEqualTo("Trichoderma harzianum");
    }

    @Test
    @DisplayName("Should return preventive routine care treatment for healthy crops")
    void testGetHealthyCareTreatments_Success() {
        List<TreatmentDto> healthyTreatments = treatmentService.getHealthyCareTreatments("POTATO");

        assertThat(healthyTreatments).isNotEmpty();
        TreatmentDto care = healthyTreatments.get(0);
        assertThat(care.treatmentType()).isEqualTo(TreatmentType.PREVENTIVE);
        assertThat(care.title()).contains("Routine Crop Care");
        assertThat(care.safetyIntervalDays()).isEqualTo(0);
    }
}
