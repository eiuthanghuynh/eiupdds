package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.diagnosis.dto.DiagnosisSaveCommand;
import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.entity.DiagnosisStatus;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.diagnosis.service.dto.PathologyResolution;
import com.eiu.pdds.diagnosis.service.impl.DiagnosisServiceImpl;
import com.eiu.pdds.diagnosis.service.impl.InMemoryDiagnosisIdempotencyService;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import com.eiu.pdds.integration.ai.AiInferenceProvider;
import com.eiu.pdds.integration.ai.dto.DetectedObject;
import com.eiu.pdds.integration.ai.dto.InferenceResult;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.storage.service.StorageService;
import com.eiu.pdds.treatment.service.TreatmentRecommendationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiagnosisSagaCleanupTest {

    private static final byte[] VALID_JPEG_BYTES = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
    };
    private static final String UPLOADED_URL = "http://localhost:9000/eiu-pdds-plants/diagnoses/2026/11/test.jpg";

    @Mock
    private AiInferenceProvider aiInferenceProvider;
    @Mock
    private StorageService storageService;
    @Mock
    private CoordinateTransformer coordinateTransformer;
    @Mock
    private PathologyAnalysisService pathologyAnalysisService;
    @Mock
    private TreatmentRecommendationService treatmentRecommendationService;
    @Mock
    private DiagnosisPersistenceService persistenceService;
    @Mock
    private DiagnosisRepository diagnosisRepository;

    private InMemoryDiagnosisIdempotencyService idempotencyService;
    private ExecutorService executorService;
    private DiagnosisServiceImpl diagnosisService;

    @BeforeEach
    void setUp() {
        idempotencyService = new InMemoryDiagnosisIdempotencyService(5);
        executorService = Executors.newVirtualThreadPerTaskExecutor();
        diagnosisService = new DiagnosisServiceImpl(
                aiInferenceProvider,
                storageService,
                coordinateTransformer,
                pathologyAnalysisService,
                treatmentRecommendationService,
                persistenceService,
                idempotencyService,
                diagnosisRepository,
                executorService
        );
    }

    @AfterEach
    void tearDown() {
        executorService.close();
    }

    private MockMultipartFile validImage(String filename) {
        return new MockMultipartFile("image", filename, "image/jpeg", VALID_JPEG_BYTES);
    }

    private InferenceResult sampleInferenceResult() {
        return new InferenceResult(
                400,
                500,
                List.of(new DetectedObject(
                        UUID.randomUUID(),
                        "Potato leaf late blight",
                        "POTATO_LATE_BLIGHT",
                        "POTATO",
                        "Potato late blight",
                        false,
                        0.88,
                        200.0,
                        250.0,
                        100.0,
                        120.0
                )),
                "{}"
        );
    }

    private PathologyResolution samplePathology() {
        Plant plant = Plant.builder().plantCode("POTATO").commonName("Potato").build();
        plant.setId(UUID.randomUUID());
        Disease disease = Disease.builder()
                .diseaseCode("POTATO_LATE_BLIGHT")
                .commonName("Potato late blight")
                .scientificName("Phytophthora infestans")
                .severity(Severity.CRITICAL)
                .build();
        disease.setId(UUID.randomUUID());
        DiseaseSummaryDto diseaseDto = new DiseaseSummaryDto(
                disease.getId(),
                "POTATO_LATE_BLIGHT",
                "Potato late blight",
                "Phytophthora infestans",
                Severity.CRITICAL
        );
        return new PathologyResolution(disease, diseaseDto, 0.88, false, List.of(), plant);
    }

    private Diagnosis savedDiagnosis(UUID id) {
        Diagnosis diagnosis = Diagnosis.builder()
                .originalImageUrl(UPLOADED_URL)
                .status(DiagnosisStatus.COMPLETED)
                .build();
        diagnosis.setId(id);
        diagnosis.setCreatedAt(Instant.now());
        return diagnosis;
    }

    @Test
    @DisplayName("Saga: When Roboflow fails even BEFORE S3 upload finishes, wait for upload and delete orphaned S3 image")
    void roboflowFailsWhileUploadInFlight_deletesUploadedImageAndRethrowsOriginalBusinessException() {
        CountDownLatch inferenceFailedLatch = new CountDownLatch(1);
        when(storageService.uploadImage(any(byte[].class), anyString(), eq("diagnoses")))
                .thenAnswer(invocation -> {
                    inferenceFailedLatch.await(2, TimeUnit.SECONDS);
                    return UPLOADED_URL;
                });
        when(aiInferenceProvider.inferPlantDisease(any(byte[].class), isNull()))
                .thenAnswer(invocation -> {
                    inferenceFailedLatch.countDown();
                    throw new BusinessException(ErrorCode.ROBOFLOW_API_ERROR, "Roboflow upstream 502");
                });

        assertThatThrownBy(() -> diagnosisService.analyzeImage(validImage("leaf.jpg"), "POTATO", null, null, "test", UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ROBOFLOW_API_ERROR);
                    assertThat(be.getMessage()).isEqualTo("Roboflow upstream 502");
                });

        verify(storageService, times(1)).deleteImage(UPLOADED_URL);
    }

    @Test
    @DisplayName("Saga: When PathologyAnalysisService throws DISEASE_NOT_FOUND, delete uploaded S3 image and rethrow")
    void pathologyThrowsDiseaseNotFound_deletesUploadedImageAndRethrows() {
        when(storageService.uploadImage(any(byte[].class), anyString(), eq("diagnoses"))).thenReturn(UPLOADED_URL);
        when(aiInferenceProvider.inferPlantDisease(any(byte[].class), isNull())).thenReturn(sampleInferenceResult());
        when(coordinateTransformer.toBoundingBoxDtos(any(), anyInt(), anyInt())).thenReturn(List.of());
        when(pathologyAnalysisService.resolvePathology(any(), eq("POTATO")))
                .thenThrow(new BusinessException(ErrorCode.DISEASE_NOT_FOUND, "Unseeded pathology"));

        assertThatThrownBy(() -> diagnosisService.analyzeImage(validImage("leaf.jpg"), "POTATO", null, null, "test", UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.DISEASE_NOT_FOUND));

        verify(storageService, times(1)).deleteImage(UPLOADED_URL);
    }

    @Test
    @DisplayName("Saga: When DiagnosisPersistenceService throws RuntimeException, delete uploaded S3 image and rethrow original")
    void persistenceThrowsRuntimeException_deletesUploadedImageAndRethrowsOriginal() {
        PathologyResolution pathology = samplePathology();
        when(storageService.uploadImage(any(byte[].class), anyString(), eq("diagnoses"))).thenReturn(UPLOADED_URL);
        when(aiInferenceProvider.inferPlantDisease(any(byte[].class), isNull())).thenReturn(sampleInferenceResult());
        when(coordinateTransformer.toBoundingBoxDtos(any(), anyInt(), anyInt())).thenReturn(List.of());
        when(pathologyAnalysisService.resolvePathology(any(), eq("POTATO"))).thenReturn(pathology);
        when(treatmentRecommendationService.getTreatmentsForDisease(any())).thenReturn(List.of());
        when(persistenceService.saveDiagnosis(any(DiagnosisSaveCommand.class)))
                .thenThrow(new IllegalStateException("DB write timeout"));

        assertThatThrownBy(() -> diagnosisService.analyzeImage(validImage("leaf.jpg"), "POTATO", null, null, "test", UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("DB write timeout");

        verify(storageService, times(1)).deleteImage(UPLOADED_URL);
    }

    @Test
    @DisplayName("Saga: When StorageService.uploadImage fails, deleteImage is never called and inference future completes cleanly")
    void uploadFails_neverCallsDeleteImageAndAwaitsInferenceCleanly() {
        AtomicBoolean inferenceCompleted = new AtomicBoolean(false);
        CountDownLatch uploadFailedLatch = new CountDownLatch(1);

        when(storageService.uploadImage(any(byte[].class), anyString(), eq("diagnoses")))
                .thenAnswer(invocation -> {
                    uploadFailedLatch.countDown();
                    throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "MinIO upload failed");
                });
        when(aiInferenceProvider.inferPlantDisease(any(byte[].class), isNull()))
                .thenAnswer(invocation -> {
                    uploadFailedLatch.await(2, TimeUnit.SECONDS);
                    inferenceCompleted.set(true);
                    return sampleInferenceResult();
                });

        assertThatThrownBy(() -> diagnosisService.analyzeImage(validImage("leaf.jpg"), "POTATO", null, null, "test", UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR);
                    assertThat(be.getMessage()).isEqualTo("MinIO upload failed");
                });

        assertThat(inferenceCompleted).isTrue();
        verify(storageService, never()).deleteImage(any());
    }

    @Test
    @DisplayName("Saga: Compensating deleteImage failure never masks original Roboflow BusinessException")
    void roboflowFailsAndDeleteThrows_originalExceptionStillRethrown() {
        when(storageService.uploadImage(any(byte[].class), anyString(), eq("diagnoses"))).thenReturn(UPLOADED_URL);
        when(aiInferenceProvider.inferPlantDisease(any(byte[].class), isNull()))
                .thenThrow(new BusinessException(ErrorCode.ROBOFLOW_API_ERROR, "Roboflow timed out"));
        doThrow(new RuntimeException("MinIO delete failed")).when(storageService).deleteImage(UPLOADED_URL);

        assertThatThrownBy(() -> diagnosisService.analyzeImage(validImage("leaf.jpg"), "POTATO", null, null, "test", UUID.randomUUID()))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ROBOFLOW_API_ERROR);
                    assertThat(be.getMessage()).isEqualTo("Roboflow timed out");
                });

        verify(storageService, times(1)).deleteImage(UPLOADED_URL);
    }

    @Test
    @DisplayName("Saga: Nested CompletionException/ExecutionException unwraps iteratively to preserve BusinessException")
    void nestedAsyncExceptions_unwrapIterativelyAndPreserveBusinessException() {
        BusinessException circuitBreakerLike = new BusinessException(ErrorCode.ACCESS_DENIED, "AI circuit open");
        when(storageService.uploadImage(any(byte[].class), anyString(), eq("diagnoses"))).thenReturn(UPLOADED_URL);
        when(aiInferenceProvider.inferPlantDisease(any(byte[].class), isNull()))
                .thenThrow(new CompletionException(new ExecutionException(circuitBreakerLike)));

        assertThatThrownBy(() -> diagnosisService.analyzeImage(validImage("leaf.jpg"), "POTATO", null, null, "test", UUID.randomUUID()))
                .isSameAs(circuitBreakerLike);

        verify(storageService, times(1)).deleteImage(UPLOADED_URL);
    }

    @Test
    @DisplayName("Idempotency: Authenticated duplicate replays cache without S3/AI; Guest duplicate runs pipeline twice without caching")
    void idempotency_authenticatedReplays_guestNeverCached() {
        PathologyResolution pathology = samplePathology();
        when(storageService.uploadImage(any(byte[].class), anyString(), eq("diagnoses"))).thenReturn(UPLOADED_URL);
        when(aiInferenceProvider.inferPlantDisease(any(byte[].class), isNull())).thenReturn(sampleInferenceResult());
        when(coordinateTransformer.toBoundingBoxDtos(any(), anyInt(), anyInt())).thenReturn(List.of());
        when(pathologyAnalysisService.resolvePathology(any(), eq("POTATO"))).thenReturn(pathology);
        when(treatmentRecommendationService.getTreatmentsForDisease(any())).thenReturn(List.of());

        UUID authDiagId = UUID.randomUUID();
        UUID guestDiagId1 = UUID.randomUUID();
        UUID guestDiagId2 = UUID.randomUUID();
        when(persistenceService.saveDiagnosis(any(DiagnosisSaveCommand.class)))
                .thenReturn(savedDiagnosis(authDiagId))
                .thenReturn(savedDiagnosis(guestDiagId1))
                .thenReturn(savedDiagnosis(guestDiagId2));

        UUID userId = UUID.randomUUID();
        DiagnosisAnalyzeResponse authFirst = diagnosisService.analyzeImage(validImage("auth1.jpg"), "POTATO", null, null, "dev", userId);
        DiagnosisAnalyzeResponse authSecond = diagnosisService.analyzeImage(validImage("auth2.jpg"), "POTATO", null, null, "dev", userId);

        assertThat(authSecond.diagnosisId()).isEqualTo(authFirst.diagnosisId());
        verify(storageService, times(1)).uploadImage(any(byte[].class), anyString(), eq("diagnoses"));
        verify(aiInferenceProvider, times(1)).inferPlantDisease(any(byte[].class), isNull());

        DiagnosisAnalyzeResponse guestFirst = diagnosisService.analyzeImage(validImage("guest1.jpg"), "POTATO", 11.0, 106.0, "guest-1", null);
        DiagnosisAnalyzeResponse guestSecond = diagnosisService.analyzeImage(validImage("guest2.jpg"), "POTATO", 12.0, 107.0, "guest-2", null);

        assertThat(guestFirst.diagnosisId()).isEqualTo(guestDiagId1);
        assertThat(guestSecond.diagnosisId()).isEqualTo(guestDiagId2);
        verify(storageService, times(3)).uploadImage(any(byte[].class), anyString(), eq("diagnoses"));
        verify(aiInferenceProvider, times(3)).inferPlantDisease(any(byte[].class), isNull());
    }
}
