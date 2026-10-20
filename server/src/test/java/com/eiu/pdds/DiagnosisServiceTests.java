package com.eiu.pdds;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.diagnosis.service.DiagnosisService;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import com.eiu.pdds.integration.roboflow.RoboflowClient;
import com.eiu.pdds.integration.roboflow.dto.RoboflowWorkflowResponse;
import com.eiu.pdds.storage.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DiagnosisServiceTests {

    @Autowired
    private DiagnosisService diagnosisService;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @MockBean
    private RoboflowClient roboflowClient;

    @MockBean
    private StorageService storageService;

    @BeforeEach
    void setUp() {
        Mockito.reset(storageService, roboflowClient);
    }

    private static final byte[] VALID_JPEG_BYTES = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
    };

    @Test
    @DisplayName("Should successfully analyze image, identify dominant pathology, and persist diagnosis")
    void testSuccessfulDiagnosis() {
        MockMultipartFile file = new MockMultipartFile("image", "potato-late-blight.jpg", "image/jpeg", VALID_JPEG_BYTES);

        when(storageService.uploadImage(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn("http://localhost:9000/eiu-pdds-plants/diagnoses/2026/10/test-leaf.jpg");

        RoboflowWorkflowResponse.ModelOutput mockOutput = new RoboflowWorkflowResponse.ModelOutput(
                new RoboflowWorkflowResponse.ImageInfo(399, 501),
                List.of(
                        new RoboflowWorkflowResponse.Prediction(322.0, 426.0, 154.0, 150.0, 0.7994, 13, "Potato leaf late blight", "det-1", "image"),
                        new RoboflowWorkflowResponse.Prediction(99.0, 336.0, 198.0, 204.0, 0.5202, 12, "Potato leaf early blight", "det-2", "image")
                )
        );

        when(roboflowClient.inferPlantDisease(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.nullable(Double.class)))
                .thenReturn(mockOutput);

        DiagnosisAnalyzeResponse response = diagnosisService.analyzeImage(file, "POTATO", 11.0528, 106.6661, "iPhone 15");

        assertThat(response).isNotNull();
        assertThat(response.diagnosisId()).isNotNull();
        assertThat(response.originalImageUrl()).isEqualTo("http://localhost:9000/eiu-pdds-plants/diagnoses/2026/10/test-leaf.jpg");
        assertThat(response.imageWidth()).isEqualTo(399);
        assertThat(response.imageHeight()).isEqualTo(501);

        // Dominant Disease Check
        assertThat(response.dominantDisease()).isNotNull();
        assertThat(response.dominantDisease().diseaseCode()).isEqualTo("POTATO_LATE_BLIGHT");
        assertThat(response.dominantConfidence()).isEqualTo(0.7994);

        // Bounding Boxes Check
        assertThat(response.boundingBoxes()).hasSize(2);
        assertThat(response.boundingBoxes().get(0).xMinNormalized()).isBetween(0.0, 1.0);
        assertThat(response.boundingBoxes().get(0).yMinNormalized()).isBetween(0.0, 1.0);

        // Co-occurring Diseases Check
        assertThat(response.coOccurringDiseases()).hasSize(1);
        assertThat(response.coOccurringDiseases().get(0).diseaseCode()).isEqualTo("POTATO_EARLY_BLIGHT");

        // Treatments Check
        assertThat(response.recommendedTreatments()).isNotEmpty();

        // Database Persistence Verification
        Optional<Diagnosis> persistedOpt = diagnosisRepository.findById(response.diagnosisId());
        assertThat(persistedOpt).isPresent();
        Diagnosis persisted = persistedOpt.get();
        assertThat(persisted.getUser()).isNull(); // Anonymous farmer in Sprint 2
        assertThat(persisted.getPlant()).isNotNull();
        assertThat(persisted.getPlant().getPlantCode()).isEqualTo("POTATO");
        assertThat(persisted.getDominantDisease()).isNotNull();
        assertThat(persisted.getDominantDisease().getDiseaseCode()).isEqualTo("POTATO_LATE_BLIGHT");
        assertThat(persisted.getBoundingBoxes()).hasSize(2);
    }

    @Test
    @DisplayName("Should handle healthy leaf detection with preventive care recommendations")
    void testHealthyLeafDetection() {
        MockMultipartFile file = new MockMultipartFile("image", "healthy-potato.jpg", "image/jpeg", VALID_JPEG_BYTES);

        when(storageService.uploadImage(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn("http://localhost:9000/eiu-pdds-plants/diagnoses/2026/10/healthy-leaf.jpg");

        RoboflowWorkflowResponse.ModelOutput mockOutput = new RoboflowWorkflowResponse.ModelOutput(
                new RoboflowWorkflowResponse.ImageInfo(400, 400),
                List.of(
                        new RoboflowWorkflowResponse.Prediction(200.0, 200.0, 300.0, 300.0, 0.92, 11, "Potato leaf", "det-healthy", "image")
                )
        );

        when(roboflowClient.inferPlantDisease(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.nullable(Double.class)))
                .thenReturn(mockOutput);

        DiagnosisAnalyzeResponse response = diagnosisService.analyzeImage(file, "POTATO", null, null, "Android Galaxy S24");

        assertThat(response).isNotNull();
        assertThat(response.dominantDisease()).isNotNull();
        assertThat(response.dominantDisease().diseaseCode()).isEqualTo("POTATO_HEALTHY");
        assertThat(response.recommendedTreatments()).hasSize(1);
        assertThat(response.recommendedTreatments().get(0).title()).contains("Routine Crop Care");

        // Database Verification
        Optional<Diagnosis> persistedOpt = diagnosisRepository.findById(response.diagnosisId());
        assertThat(persistedOpt).isPresent();
        assertThat(persistedOpt.get().getDominantDisease()).isNull(); // No pathology entity linked
        assertThat(persistedOpt.get().getBoundingBoxes()).hasSize(1);
    }

    @Test
    @DisplayName("Should handle zero detections gracefully without throwing error")
    void testZeroDetectionsGracefullyHandled() {
        MockMultipartFile file = new MockMultipartFile("image", "blurry.jpg", "image/jpeg", VALID_JPEG_BYTES);

        when(storageService.uploadImage(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn("http://localhost:9000/eiu-pdds-plants/diagnoses/2026/10/blurry.jpg");

        RoboflowWorkflowResponse.ModelOutput mockOutput = new RoboflowWorkflowResponse.ModelOutput(
                new RoboflowWorkflowResponse.ImageInfo(300, 300),
                List.of()
        );

        when(roboflowClient.inferPlantDisease(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.nullable(Double.class)))
                .thenReturn(mockOutput);

        DiagnosisAnalyzeResponse response = diagnosisService.analyzeImage(file, "TOMATO", null, null, null);

        assertThat(response).isNotNull();
        assertThat(response.dominantDisease()).isNull();
        assertThat(response.dominantConfidence()).isNull();
        assertThat(response.boundingBoxes()).isEmpty();
        assertThat(response.recommendedTreatments()).isEmpty();

        // Database Verification
        Optional<Diagnosis> persistedOpt = diagnosisRepository.findById(response.diagnosisId());
        assertThat(persistedOpt).isPresent();
        assertThat(persistedOpt.get().getStatus().name()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("Should throw RESOURCE_NOT_FOUND (404) if dominant disease is not registered in database catalog")
    void testUnseededDiseaseThrowsResourceNotFound() {
        MockMultipartFile file = new MockMultipartFile("image", "unknown-disease.jpg", "image/jpeg", VALID_JPEG_BYTES);

        when(storageService.uploadImage(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn("http://localhost:9000/eiu-pdds-plants/diagnoses/2026/10/unknown.jpg");

        // CORN_GRAY_LEAF_SPOT is recognized by mapper, but not seeded in V2 migration / MasterDataSeeder
        RoboflowWorkflowResponse.ModelOutput mockOutput = new RoboflowWorkflowResponse.ModelOutput(
                new RoboflowWorkflowResponse.ImageInfo(300, 300),
                List.of(
                        new RoboflowWorkflowResponse.Prediction(150.0, 150.0, 100.0, 100.0, 0.85, 4, "Corn Gray leaf spot", "det-unseeded", "image")
                )
        );

        when(roboflowClient.inferPlantDisease(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.nullable(Double.class)))
                .thenReturn(mockOutput);

        assertThatThrownBy(() -> diagnosisService.analyzeImage(file, "CORN", null, null, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.DISEASE_NOT_FOUND));
    }

    @Test
    @DisplayName("Should throw INVALID_FILE_TYPE when uploading non-image corrupted bytes")
    void testInvalidFileSignatureThrowsException() {
        byte[] fakeTextBytes = "This is a plain text file, not a leaf image".getBytes();
        MockMultipartFile fakeFile = new MockMultipartFile("image", "fake.jpg", "image/jpeg", fakeTextBytes);

        assertThatThrownBy(() -> diagnosisService.analyzeImage(fakeFile, "POTATO", null, null, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_FILE_TYPE));
    }
}
