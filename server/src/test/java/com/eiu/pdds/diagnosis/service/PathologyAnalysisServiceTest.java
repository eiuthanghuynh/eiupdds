package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.diagnosis.service.dto.PathologyResolution;
import com.eiu.pdds.diagnosis.service.impl.PathologyAnalysisServiceImpl;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.integration.ai.dto.DetectedObject;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.plant.repository.PlantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PathologyAnalysisServiceTest {

    @Mock
    private DiseaseRepository diseaseRepository;

    @Mock
    private PlantRepository plantRepository;

    private PathologyAnalysisService pathologyService;

    @BeforeEach
    void setUp() {
        pathologyService = new PathologyAnalysisServiceImpl(diseaseRepository, plantRepository);
    }

    @Test
    @DisplayName("Should correctly identify dominant disease and co-occurring diseases using Max Confidence")
    void testResolvePathology_DominantAndCoOccurring() {
        DetectedObject dominant = new DetectedObject(
                UUID.randomUUID(),
                "Potato leaf late blight",
                "POTATO_LATE_BLIGHT",
                "POTATO",
                "Potato late blight",
                false,
                0.85, 100, 100, 50, 50
        );
        DetectedObject secondary = new DetectedObject(
                UUID.randomUUID(),
                "Potato leaf early blight",
                "POTATO_EARLY_BLIGHT",
                "POTATO",
                "Potato early blight",
                false,
                0.60, 200, 200, 60, 60
        );

        Disease lateBlight = Disease.builder()
                .diseaseCode("POTATO_LATE_BLIGHT")
                .commonName("Potato late blight")
                .scientificName("Phytophthora infestans")
                .severity(Severity.CRITICAL)
                .build();
        lateBlight.setId(UUID.randomUUID());

        Disease earlyBlight = Disease.builder()
                .diseaseCode("POTATO_EARLY_BLIGHT")
                .commonName("Potato early blight")
                .scientificName("Alternaria solani")
                .severity(Severity.HIGH)
                .build();
        earlyBlight.setId(UUID.randomUUID());

        Plant potatoPlant = Plant.builder()
                .plantCode("POTATO")
                .commonName("Potato")
                .build();

        when(diseaseRepository.findByDiseaseCode("POTATO_LATE_BLIGHT")).thenReturn(Optional.of(lateBlight));
        when(diseaseRepository.findByDiseaseCode("POTATO_EARLY_BLIGHT")).thenReturn(Optional.of(earlyBlight));
        when(plantRepository.findByPlantCode("POTATO")).thenReturn(Optional.of(potatoPlant));

        PathologyResolution resolution = pathologyService.resolvePathology(List.of(dominant, secondary), "POTATO");

        assertThat(resolution.isHealthy()).isFalse();
        assertThat(resolution.dominantDiseaseDto().diseaseCode()).isEqualTo("POTATO_LATE_BLIGHT");
        assertThat(resolution.dominantConfidence()).isEqualTo(0.85);
        assertThat(resolution.dominantDiseaseEntity()).isEqualTo(lateBlight);

        assertThat(resolution.coOccurringDiseases()).hasSize(1);
        assertThat(resolution.coOccurringDiseases().get(0).diseaseCode()).isEqualTo("POTATO_EARLY_BLIGHT");
    }

    @Test
    @DisplayName("Should detect healthy foliage when dominant detection is healthy")
    void testResolvePathology_HealthyFoliage() {
        DetectedObject healthyLeaf = new DetectedObject(
                UUID.randomUUID(),
                "Tomato leaf",
                "TOMATO_HEALTHY",
                "TOMATO",
                "Healthy tomato leaf",
                true,
                0.92, 100, 100, 50, 50
        );
        Plant tomatoPlant = Plant.builder().plantCode("TOMATO").commonName("Tomato").build();

        when(plantRepository.findByPlantCode("TOMATO")).thenReturn(Optional.of(tomatoPlant));

        PathologyResolution resolution = pathologyService.resolvePathology(List.of(healthyLeaf), "TOMATO");

        assertThat(resolution.isHealthy()).isTrue();
        assertThat(resolution.dominantDiseaseDto().diseaseCode()).isEqualTo("TOMATO_HEALTHY");
        assertThat(resolution.dominantConfidence()).isEqualTo(0.92);
        assertThat(resolution.dominantDiseaseEntity()).isNull();
        assertThat(resolution.coOccurringDiseases()).isEmpty();
    }

    @Test
    @DisplayName("Should throw BusinessException with 404 when dominant disease is unseeded")
    void testResolvePathology_UnseededDisease_ThrowsException() {
        DetectedObject unknownDisease = new DetectedObject(
                UUID.randomUUID(),
                "Corn rust leaf",
                "CORN_COMMON_RUST",
                "CORN",
                "Corn common rust",
                false,
                0.88, 100, 100, 50, 50
        );

        when(diseaseRepository.findByDiseaseCode("CORN_COMMON_RUST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pathologyService.resolvePathology(List.of(unknownDisease), "CORN"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.DISEASE_NOT_FOUND));
    }
}
