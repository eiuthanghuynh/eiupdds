package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.diagnosis.service.impl.DiagnosisPersistenceServiceImpl;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.dto.diagnosis.BoundingBoxDto;
import com.eiu.pdds.plant.entity.Plant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosisPersistenceServiceTest {

    @Mock
    private DiagnosisRepository diagnosisRepository;

    @Mock
    private DiseaseRepository diseaseRepository;

    private DiagnosisPersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService = new DiagnosisPersistenceServiceImpl(diagnosisRepository, diseaseRepository);
    }

    @Test
    @DisplayName("Should successfully persist Diagnosis entity with cascaded BoundingBoxes")
    void testSaveDiagnosis_Success() {
        Plant plant = Plant.builder().plantCode("POTATO").build();
        Disease disease = Disease.builder().diseaseCode("POTATO_LATE_BLIGHT").build();
        disease.setId(UUID.randomUUID());

        when(diseaseRepository.findByDiseaseCode("POTATO_LATE_BLIGHT")).thenReturn(Optional.of(disease));
        when(diagnosisRepository.save(any(Diagnosis.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BoundingBoxDto boxDto = new BoundingBoxDto(
                UUID.randomUUID(),
                "Potato leaf late blight",
                "POTATO_LATE_BLIGHT",
                0.85,
                100.0, 100.0, 50.0, 50.0,
                75.0, 75.0,
                0.25, 0.25, 0.15, 0.15
        );

        Diagnosis saved = persistenceService.saveDiagnosis(
                plant,
                "http://s3/image.jpg",
                400,
                400,
                11.0,
                106.0,
                "iPhone 15",
                disease,
                0.85,
                150,
                "{}",
                List.of(boxDto)
        );

        assertThat(saved).isNotNull();
        assertThat(saved.getOriginalImageUrl()).isEqualTo("http://s3/image.jpg");
        assertThat(saved.getBoundingBoxes()).hasSize(1);
        assertThat(saved.getBoundingBoxes().get(0).getDisease()).isEqualTo(disease);

        ArgumentCaptor<Diagnosis> captor = ArgumentCaptor.forClass(Diagnosis.class);
        verify(diagnosisRepository).save(captor.capture());
        assertThat(captor.getValue().getDeviceInfo()).isEqualTo("iPhone 15");
    }
}
