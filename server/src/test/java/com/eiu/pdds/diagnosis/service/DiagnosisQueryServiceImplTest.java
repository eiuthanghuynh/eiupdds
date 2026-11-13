package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.common.api.PageResponse;
import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.diagnosis.entity.BoundingBox;
import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.entity.DiagnosisStatus;
import com.eiu.pdds.diagnosis.repository.BoundingBoxRepository;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.diagnosis.service.impl.DiagnosisQueryServiceImpl;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.dto.diagnosis.BoundingBoxDto;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import com.eiu.pdds.dto.diagnosis.DiagnosisHistoryFilter;
import com.eiu.pdds.dto.diagnosis.DiagnosisSummaryDto;
import com.eiu.pdds.dto.treatment.TreatmentDto;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.treatment.entity.TreatmentType;
import com.eiu.pdds.treatment.service.TreatmentRecommendationService;
import com.eiu.pdds.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosisQueryServiceImplTest {

    @Mock
    private DiagnosisRepository diagnosisRepository;

    @Mock
    private BoundingBoxRepository boundingBoxRepository;

    @Mock
    private CoordinateTransformer coordinateTransformer;

    @Mock
    private TreatmentRecommendationService treatmentRecommendationService;

    private DiagnosisQueryService queryService;

    private UUID userId;
    private User testUser;
    private Plant testPlant;
    private Disease testDisease;

    @BeforeEach
    void setUp() {
        queryService = new DiagnosisQueryServiceImpl(
                diagnosisRepository,
                boundingBoxRepository,
                coordinateTransformer,
                treatmentRecommendationService
        );

        userId = UUID.randomUUID();
        testUser = User.builder().build();
        testUser.setId(userId);

        testPlant = Plant.builder()
                .plantCode("POTATO")
                .commonName("Potato")
                .build();
        testPlant.setId(UUID.randomUUID());

        testDisease = Disease.builder()
                .diseaseCode("POTATO_LATE_BLIGHT")
                .commonName("Potato Late Blight")
                .scientificName("Phytophthora infestans")
                .severity(Severity.CRITICAL)
                .build();
        testDisease.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("getUserHistory - Should return paginated summaries with bounding box counts")
    void testGetUserHistory_Success() {
        UUID diagnosisId = UUID.randomUUID();
        Diagnosis diagnosis = Diagnosis.builder()
                .user(testUser)
                .plant(testPlant)
                .dominantDisease(testDisease)
                .dominantConfidence(0.85)
                .originalImageUrl("https://s3/sample.jpg")
                .latitude(11.0)
                .longitude(106.0)
                .deviceInfo("iPhone")
                .status(DiagnosisStatus.COMPLETED)
                .build();
        diagnosis.setId(diagnosisId);
        diagnosis.setCreatedAt(Instant.now());

        Pageable pageable = PageRequest.of(0, 10);
        Page<Diagnosis> page = new PageImpl<>(List.of(diagnosis), pageable, 1);

        when(diagnosisRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        List<Object[]> mockCounts = Collections.singletonList(new Object[]{diagnosisId, 3L});
        when(boundingBoxRepository.countByDiagnosisIds(List.of(diagnosisId))).thenReturn(mockCounts);

        DiagnosisHistoryFilter filter = new DiagnosisHistoryFilter("POTATO", null, null, null, null);
        PageResponse<DiagnosisSummaryDto> result = queryService.getUserHistory(userId, filter, pageable);

        assertThat(result).isNotNull();
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);

        DiagnosisSummaryDto summary = result.content().get(0);
        assertThat(summary.diagnosisId()).isEqualTo(diagnosisId);
        assertThat(summary.boundingBoxCount()).isEqualTo(3);
        assertThat(summary.plant().plantCode()).isEqualTo("POTATO");
        assertThat(summary.dominantDisease().diseaseCode()).isEqualTo("POTATO_LATE_BLIGHT");
        assertThat(summary.dominantConfidence()).isEqualTo(0.85);
    }

    @Test
    @DisplayName("getUserHistory - Should throw exception when userId is null")
    void testGetUserHistory_NullUserIdThrows() {
        Pageable pageable = PageRequest.of(0, 10);
        assertThatThrownBy(() -> queryService.getUserHistory(null, null, pageable))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_PAYLOAD));
    }

    @Test
    @DisplayName("getUserDiagnosisDetail - Should return full session response with dual coordinates")
    void testGetUserDiagnosisDetail_Success() {
        UUID diagnosisId = UUID.randomUUID();
        BoundingBox box = BoundingBox.builder()
                .disease(testDisease)
                .label("Potato leaf late blight")
                .confidence(0.85)
                .x(100.0)
                .y(100.0)
                .width(50.0)
                .height(50.0)
                .build();
        box.setId(UUID.randomUUID());

        Diagnosis diagnosis = Diagnosis.builder()
                .user(testUser)
                .plant(testPlant)
                .dominantDisease(testDisease)
                .dominantConfidence(0.85)
                .originalImageUrl("https://s3/sample.jpg")
                .imageWidth(800)
                .imageHeight(600)
                .inferenceTimeMs(120)
                .status(DiagnosisStatus.COMPLETED)
                .build();
        diagnosis.setId(diagnosisId);
        diagnosis.addBoundingBox(box);

        when(diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull(diagnosisId, userId))
                .thenReturn(Optional.of(diagnosis));

        BoundingBoxDto boxDto = new BoundingBoxDto(
                box.getId(),
                box.getLabel(),
                "POTATO_LATE_BLIGHT",
                0.85,
                100.0, 100.0, 50.0, 50.0,
                75.0, 75.0,
                0.0938, 0.125, 0.0625, 0.0833
        );
        when(coordinateTransformer.toBoundingBoxDtosFromEntities(any(), eq(800), eq(600)))
                .thenReturn(List.of(boxDto));

        TreatmentDto treatment = new TreatmentDto(
                UUID.randomUUID(),
                TreatmentType.CHEMICAL,
                "Fungicide treatment",
                "Apply Mancozeb",
                "Mancozeb",
                "2g/L",
                7,
                "Wear protective gear"
        );
        when(treatmentRecommendationService.getTreatmentsForDisease(testDisease.getId()))
                .thenReturn(List.of(treatment));

        DiagnosisAnalyzeResponse response = queryService.getUserDiagnosisDetail(userId, diagnosisId);

        assertThat(response).isNotNull();
        assertThat(response.diagnosisId()).isEqualTo(diagnosisId);
        assertThat(response.imageWidth()).isEqualTo(800);
        assertThat(response.imageHeight()).isEqualTo(600);
        assertThat(response.dominantDisease().diseaseCode()).isEqualTo("POTATO_LATE_BLIGHT");
        assertThat(response.boundingBoxes()).hasSize(1);
        assertThat(response.recommendedTreatments()).hasSize(1);
        assertThat(response.recommendedTreatments().get(0).title()).isEqualTo("Fungicide treatment");
    }

    @Test
    @DisplayName("getUserDiagnosisDetail - Should throw 404 DIAGNOSIS_NOT_FOUND when record belongs to different user (Zero-Trust IDOR)")
    void testGetUserDiagnosisDetail_IdorProtection() {
        UUID diagnosisId = UUID.randomUUID();
        when(diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull(diagnosisId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getUserDiagnosisDetail(userId, diagnosisId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.DIAGNOSIS_NOT_FOUND));
    }
}
