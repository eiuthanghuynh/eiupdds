package com.eiu.pdds.diagnosis.controller;

import com.eiu.pdds.auth.security.JwtTokenProvider;
import com.eiu.pdds.diagnosis.entity.BoundingBox;
import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.entity.DiagnosisStatus;
import com.eiu.pdds.diagnosis.repository.BoundingBoxRepository;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.integration.roboflow.RoboflowClient;
import com.eiu.pdds.integration.roboflow.dto.RoboflowWorkflowResponse;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.plant.repository.PlantRepository;
import com.eiu.pdds.storage.service.StorageService;
import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DiagnosisHistoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlantRepository plantRepository;

    @Autowired
    private DiseaseRepository diseaseRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private BoundingBoxRepository boundingBoxRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private com.eiu.pdds.diagnosis.service.DiagnosisIdempotencyService idempotencyService;

    @MockBean
    private RoboflowClient roboflowClient;

    @MockBean
    private StorageService storageService;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;
    private Plant potatoPlant;
    private Disease lateBlightDisease;

    @BeforeEach
    void setUp() {
        Mockito.reset(storageService, roboflowClient);

        boundingBoxRepository.deleteAll();
        diagnosisRepository.deleteAll();

        userA = userRepository.findByUsername("user_a")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("user_a")
                        .email("user_a@farm.vn")
                        .passwordHash("hash")
                        .role(Role.ROLE_USER)
                        .isActive(true)
                        .build()));

        userB = userRepository.findByUsername("user_b")
                .orElseGet(() -> userRepository.save(User.builder()
                        .username("user_b")
                        .email("user_b@farm.vn")
                        .passwordHash("hash")
                        .role(Role.ROLE_USER)
                        .isActive(true)
                        .build()));

        tokenA = jwtTokenProvider.generateAccessToken(userA);
        tokenB = jwtTokenProvider.generateAccessToken(userB);

        potatoPlant = plantRepository.findByPlantCode("POTATO").orElseThrow();
        lateBlightDisease = diseaseRepository.findByDiseaseCode("POTATO_LATE_BLIGHT").orElseThrow();
    }

    @Test
    @DisplayName("GET /api/v1/diagnoses - Should reject unauthenticated requests with 401")
    void testGetHistoryUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/diagnoses"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_001")));
    }

    @Test
    @DisplayName("POST /api/v1/diagnoses/analyze - Should link authenticated user and GPS coordinates")
    void testAnalyzeWithAuthenticatedUserLinksHistory() throws Exception {
        byte[] validJpegBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        };

        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "leaf.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                validJpegBytes
        );

        when(storageService.uploadImage(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn("https://s3.eiu.edu.vn/eiu-pdds-plants/leaf.jpg");

        RoboflowWorkflowResponse.ModelOutput mockOutput = new RoboflowWorkflowResponse.ModelOutput(
                new RoboflowWorkflowResponse.ImageInfo(400, 500),
                List.of(
                        new RoboflowWorkflowResponse.Prediction(200.0, 250.0, 100.0, 100.0, 0.88, 13, "Potato leaf late blight", "det-1", "image")
                )
        );

        when(roboflowClient.inferPlantDisease(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.nullable(Double.class)))
                .thenReturn(mockOutput);

        mockMvc.perform(multipart("/api/v1/diagnoses/analyze")
                        .file(imageFile)
                        .header("Authorization", "Bearer " + tokenA)
                        .param("plantCode", "POTATO")
                        .param("latitude", "11.0528")
                        .param("longitude", "106.6661")
                        .param("deviceInfo", "iPhone 15 Pro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.diagnosisId", notNullValue()))
                .andExpect(jsonPath("$.data.plant.plantCode", is("POTATO")))
                .andExpect(jsonPath("$.data.latitude", is(11.0528)))
                .andExpect(jsonPath("$.data.longitude", is(106.6661)))
                .andExpect(jsonPath("$.data.deviceInfo", is("iPhone 15 Pro")));

        List<Diagnosis> userADiagnoses = diagnosisRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userA.getId());
        assertThat(userADiagnoses).hasSize(1);
        Diagnosis saved = userADiagnoses.get(0);
        assertThat(saved.getUser().getId()).isEqualTo(userA.getId());
        assertThat(saved.getLatitude()).isEqualTo(11.0528);
        assertThat(saved.getLongitude()).isEqualTo(106.6661);
        assertThat(saved.getDeviceInfo()).isEqualTo("iPhone 15 Pro");
    }

    @Test
    @DisplayName("GET /api/v1/diagnoses - User A should only see own history and not User B's")
    void testUserIsolationOnHistoryList() throws Exception {
        Diagnosis diagA = Diagnosis.builder()
                .user(userA)
                .plant(potatoPlant)
                .dominantDisease(lateBlightDisease)
                .dominantConfidence(0.85)
                .originalImageUrl("https://s3/leaf_a.jpg")
                .status(DiagnosisStatus.COMPLETED)
                .build();
        diagA = diagnosisRepository.save(diagA);

        BoundingBox boxA = BoundingBox.builder()
                .label("Potato leaf late blight")
                .disease(lateBlightDisease)
                .confidence(0.85)
                .x(100.0).y(100.0).width(50.0).height(50.0)
                .build();
        diagA.addBoundingBox(boxA);
        diagnosisRepository.save(diagA);

        Diagnosis diagB = Diagnosis.builder()
                .user(userB)
                .plant(potatoPlant)
                .dominantDisease(lateBlightDisease)
                .dominantConfidence(0.92)
                .originalImageUrl("https://s3/leaf_b.jpg")
                .status(DiagnosisStatus.COMPLETED)
                .build();
        diagB = diagnosisRepository.save(diagB);

        // User A requests history
        mockMvc.perform(get("/api/v1/diagnoses")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].diagnosisId", is(diagA.getId().toString())))
                .andExpect(jsonPath("$.data.content[0].boundingBoxCount", is(1)))
                .andExpect(jsonPath("$.data.content[0].plant.plantCode", is("POTATO")));

        // User B requests history
        mockMvc.perform(get("/api/v1/diagnoses")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].diagnosisId", is(diagB.getId().toString())));
    }

    @Test
    @DisplayName("GET /api/v1/diagnoses/{id} - Should return full details for owner; return 404 for another user (Zero-Trust IDOR)")
    void testDiagnosisDetailAndIdorProtection() throws Exception {
        Diagnosis diagA = Diagnosis.builder()
                .user(userA)
                .plant(potatoPlant)
                .dominantDisease(lateBlightDisease)
                .dominantConfidence(0.85)
                .originalImageUrl("https://s3/leaf_a.jpg")
                .imageWidth(800)
                .imageHeight(600)
                .inferenceTimeMs(150)
                .latitude(11.0528)
                .longitude(106.6661)
                .deviceInfo("iPhone 15 Pro")
                .status(DiagnosisStatus.COMPLETED)
                .build();
        diagA = diagnosisRepository.save(diagA);

        BoundingBox boxA = BoundingBox.builder()
                .label("Potato leaf late blight")
                .disease(lateBlightDisease)
                .confidence(0.85)
                .x(200.0).y(200.0).width(100.0).height(100.0)
                .build();
        diagA.addBoundingBox(boxA);
        diagA = diagnosisRepository.save(diagA);

        // Owner (User A) accesses -> 200 OK
        mockMvc.perform(get("/api/v1/diagnoses/" + diagA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.diagnosisId", is(diagA.getId().toString())))
                .andExpect(jsonPath("$.data.imageWidth", is(800)))
                .andExpect(jsonPath("$.data.imageHeight", is(600)))
                .andExpect(jsonPath("$.data.plant.plantCode", is("POTATO")))
                .andExpect(jsonPath("$.data.latitude", is(11.0528)))
                .andExpect(jsonPath("$.data.longitude", is(106.6661)))
                .andExpect(jsonPath("$.data.deviceInfo", is("iPhone 15 Pro")))
                .andExpect(jsonPath("$.data.dominantDisease.diseaseCode", is("POTATO_LATE_BLIGHT")))
                .andExpect(jsonPath("$.data.boundingBoxes", hasSize(1)))
                .andExpect(jsonPath("$.data.boundingBoxes[0].xMinNormalized", notNullValue()))
                .andExpect(jsonPath("$.data.recommendedTreatments", hasSize(notNullValue())));

        // Another user (User B) attempts to access User A's diagnosis -> 404 DIAGNOSIS_NOT_FOUND (IDOR Protection)
        mockMvc.perform(get("/api/v1/diagnoses/" + diagA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DIAGNOSIS_001")));
    }

    @Test
    @DisplayName("DELETE /api/v1/diagnoses/{id} - Soft deletes record; subsequent queries exclude deleted record")
    void testSoftDeleteDiagnosis() throws Exception {
        Diagnosis diagA = Diagnosis.builder()
                .user(userA)
                .plant(potatoPlant)
                .dominantDisease(lateBlightDisease)
                .dominantConfidence(0.85)
                .originalImageUrl("https://s3/leaf_a.jpg")
                .status(DiagnosisStatus.COMPLETED)
                .build();
        diagA = diagnosisRepository.save(diagA);

        // User B attempts to delete User A's record -> 404
        mockMvc.perform(delete("/api/v1/diagnoses/" + diagA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DIAGNOSIS_001")));

        // User A soft deletes own record -> 200 OK
        mockMvc.perform(delete("/api/v1/diagnoses/" + diagA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Subsequent GET detail by User A -> 404 Not Found
        mockMvc.perform(get("/api/v1/diagnoses/" + diagA.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DIAGNOSIS_001")));

        // Subsequent GET list by User A -> empty (0 records)
        mockMvc.perform(get("/api/v1/diagnoses")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(0)));

        // Verify entity still exists in DB as tombstone (soft deleted)
        Diagnosis inDb = diagnosisRepository.findById(diagA.getId()).orElseThrow();
        assertThat(inDb.isDeleted()).isTrue();
        assertThat(inDb.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("GET /api/v1/diagnoses with filters - Should filter by plantCode, diseaseCode, status, and date range")
    void testGetHistoryWithFilters() throws Exception {
        Plant tomatoPlant = plantRepository.findByPlantCode("TOMATO").orElseThrow();
        Disease earlyBlightDisease = diseaseRepository.findByDiseaseCode("POTATO_EARLY_BLIGHT").orElseThrow();

        // 1. Potato Late Blight (COMPLETED)
        Diagnosis diag1 = Diagnosis.builder()
                .user(userA)
                .plant(potatoPlant)
                .dominantDisease(lateBlightDisease)
                .dominantConfidence(0.85)
                .originalImageUrl("https://s3/leaf1.jpg")
                .status(DiagnosisStatus.COMPLETED)
                .build();
        diag1.addBoundingBox(BoundingBox.builder()
                .label("Potato leaf late blight")
                .disease(lateBlightDisease)
                .confidence(0.85)
                .x(100.0).y(100.0).width(50.0).height(50.0)
                .build());
        diag1 = diagnosisRepository.save(diag1);

        // 2. Potato Healthy (COMPLETED, dominantDisease = null)
        Diagnosis diag2 = Diagnosis.builder()
                .user(userA)
                .plant(potatoPlant)
                .dominantConfidence(0.95)
                .originalImageUrl("https://s3/leaf2.jpg")
                .status(DiagnosisStatus.COMPLETED)
                .build();
        diag2.addBoundingBox(BoundingBox.builder()
                .label("Potato leaf")
                .confidence(0.95)
                .x(100.0).y(100.0).width(50.0).height(50.0)
                .build());
        diag2 = diagnosisRepository.save(diag2);

        // 3. Tomato Early Blight (PENDING)
        Diagnosis diag3 = Diagnosis.builder()
                .user(userA)
                .plant(tomatoPlant)
                .dominantDisease(earlyBlightDisease)
                .dominantConfidence(0.70)
                .originalImageUrl("https://s3/leaf3.jpg")
                .status(DiagnosisStatus.PENDING)
                .build();
        diag3.addBoundingBox(BoundingBox.builder()
                .label("Tomato leaf")
                .confidence(0.70)
                .x(100.0).y(100.0).width(50.0).height(50.0)
                .build());
        diag3 = diagnosisRepository.save(diag3);

        // Filter by plantCode=POTATO -> matches diag1, diag2
        mockMvc.perform(get("/api/v1/diagnoses")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("plantCode", "POTATO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(2)));

        // Filter by diseaseCode=POTATO_LATE_BLIGHT -> matches diag1
        mockMvc.perform(get("/api/v1/diagnoses")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("diseaseCode", "POTATO_LATE_BLIGHT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].dominantDisease.diseaseCode", is("POTATO_LATE_BLIGHT")));

        // Filter by diseaseCode=POTATO_HEALTHY -> matches diag2
        mockMvc.perform(get("/api/v1/diagnoses")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("diseaseCode", "POTATO_HEALTHY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(1)))
                .andExpect(jsonPath("$.data.content[0].diagnosisId", is(diag2.getId().toString())))
                .andExpect(jsonPath("$.data.content[0].dominantDisease.diseaseCode", is("POTATO_HEALTHY")));

        // Filter by status=COMPLETED -> matches diag1, diag2
        mockMvc.perform(get("/api/v1/diagnoses")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(2)));

        // Filter by date range covering now -> matches all 3
        Instant from = Instant.now().minus(Duration.ofHours(2));
        Instant to = Instant.now().plus(Duration.ofHours(2));
        mockMvc.perform(get("/api/v1/diagnoses")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("fromDate", from.toString())
                        .param("toDate", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(3)));

        // Filter by future date range -> matches 0
        Instant future = Instant.now().plus(Duration.ofDays(2));
        mockMvc.perform(get("/api/v1/diagnoses")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("fromDate", future.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements", is(0)));
    }

    @Test
    @DisplayName("Lifecycle: Soft delete evicts idempotency cache allowing fresh re-analysis of identical image")
    void testSoftDeleteEvictsIdempotencyCacheAndAllowsReanalysis() throws Exception {
        byte[] validJpegBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x02
        };

        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "reanalysis_leaf.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                validJpegBytes
        );

        when(storageService.uploadImage(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn("https://s3.eiu.edu.vn/eiu-pdds-plants/reanalysis.jpg");

        RoboflowWorkflowResponse.ModelOutput mockOutput = new RoboflowWorkflowResponse.ModelOutput(
                new RoboflowWorkflowResponse.ImageInfo(400, 500),
                List.of(
                        new RoboflowWorkflowResponse.Prediction(200.0, 250.0, 100.0, 100.0, 0.88, 13, "Potato leaf late blight", "det-1", "image")
                )
        );

        when(roboflowClient.inferPlantDisease(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.nullable(Double.class)))
                .thenReturn(mockOutput);

        // 1. Initial Analysis
        String firstResponseStr = mockMvc.perform(multipart("/api/v1/diagnoses/analyze")
                        .file(imageFile)
                        .header("Authorization", "Bearer " + tokenA)
                        .param("plantCode", "POTATO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode rootNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(firstResponseStr);
        String firstDiagnosisId = rootNode.get("data").get("diagnosisId").asText();

        // 2. Soft-delete the diagnosis
        mockMvc.perform(delete("/api/v1/diagnoses/" + firstDiagnosisId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 3. Re-analyze the exact same image bytes
        MockMultipartFile identicalImageFile = new MockMultipartFile(
                "image",
                "reanalysis_leaf.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                validJpegBytes
        );

        String secondResponseStr = mockMvc.perform(multipart("/api/v1/diagnoses/analyze")
                        .file(identicalImageFile)
                        .header("Authorization", "Bearer " + tokenA)
                        .param("plantCode", "POTATO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode secondRootNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(secondResponseStr);
        String secondDiagnosisId = secondRootNode.get("data").get("diagnosisId").asText();

        // 4. Assert that a fresh diagnosis session was created rather than replaying the soft-deleted one
        assertThat(secondDiagnosisId).isNotEqualTo(firstDiagnosisId);
    }
}
