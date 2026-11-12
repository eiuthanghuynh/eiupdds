package com.eiu.pdds.diagnosis.controller;

import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.repository.BoundingBoxRepository;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.integration.roboflow.RoboflowClient;
import com.eiu.pdds.integration.roboflow.dto.RoboflowWorkflowResponse;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.plant.repository.PlantRepository;
import com.eiu.pdds.storage.service.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test suite for Guest Diagnosis Mode (Task 4.1).
 *
 * <p>Validates that anonymous/guest callers can perform AI leaf disease detection
 * via public {@code POST /api/v1/diagnoses/analyze} without a Bearer token, persisting
 * an anonymous record (user_id = null), while all history query and deletion endpoints
 * continue to strictly require authentication (401 Unauthorized).
 *
 * <p>Phase: TDD RED Phase (Task 4.1).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class GuestDiagnosisIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlantRepository plantRepository;

    @Autowired
    private DiseaseRepository diseaseRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private BoundingBoxRepository boundingBoxRepository;

    @MockBean
    private RoboflowClient roboflowClient;

    @MockBean
    private StorageService storageService;

    private Plant potatoPlant;
    private Disease lateBlightDisease;

    @BeforeEach
    void setUp() {
        Mockito.reset(storageService, roboflowClient);

        boundingBoxRepository.deleteAll();
        diagnosisRepository.deleteAll();

        potatoPlant = plantRepository.findByPlantCode("POTATO")
                .orElseGet(() -> plantRepository.save(Plant.builder()
                        .plantCode("POTATO")
                        .commonName("Potato")
                        .scientificName("Solanum tuberosum")
                        .iconUrl("https://cdn.eiu.edu.vn/icons/potato.svg")
                        .description("Tuber crop")
                        .build()));

        lateBlightDisease = diseaseRepository.findByDiseaseCode("POTATO_LATE_BLIGHT")
                .orElseGet(() -> diseaseRepository.save(Disease.builder()
                        .diseaseCode("POTATO_LATE_BLIGHT")
                        .commonName("Potato late blight")
                        .scientificName("Phytophthora infestans")
                        .severity(Severity.CRITICAL)
                        .symptoms("Dark brown to black water-soaked lesions.")
                        .description("Destructive fungal-like oomycete disease.")
                        .build()));
    }

    // =========================================================================
    // 1. Guest Leaf Image Inference (Public Access)
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/diagnoses/analyze - Guest mode: Anonymous request without Bearer token returns 200 OK and persists user_id=null")
    void testGuestDiagnosis_AnalyzeSuccess_WithoutBearerToken_PersistsAnonymousRecord() throws Exception {
        byte[] validJpegBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        };

        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "guest_leaf.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                validJpegBytes
        );

        when(storageService.uploadImage(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn("https://s3.eiu.edu.vn/eiu-pdds-plants/guest_leaf.jpg");

        RoboflowWorkflowResponse.ModelOutput mockOutput = new RoboflowWorkflowResponse.ModelOutput(
                new RoboflowWorkflowResponse.ImageInfo(400, 500),
                List.of(
                        new RoboflowWorkflowResponse.Prediction(200.0, 250.0, 100.0, 100.0, 0.88, 13, "Potato leaf late blight", "det-1", "image")
                )
        );

        when(roboflowClient.inferPlantDisease(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.nullable(Double.class)))
                .thenReturn(mockOutput);

        // Perform analyze request WITHOUT Authorization header
        MvcResult mvcResult = mockMvc.perform(multipart("/api/v1/diagnoses/analyze")
                        .file(imageFile)
                        .param("plantCode", "POTATO")
                        .param("latitude", "11.0528")
                        .param("longitude", "106.6661")
                        .param("deviceInfo", "Guest Mobile Browser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.diagnosisId", notNullValue()))
                .andExpect(jsonPath("$.data.plant.plantCode", is("POTATO")))
                .andExpect(jsonPath("$.data.dominantDisease.diseaseCode", is("POTATO_LATE_BLIGHT")))
                .andExpect(jsonPath("$.data.boundingBoxes", notNullValue()))
                .andReturn();

        // Verify database persistence: user entity MUST be null for anonymous guest diagnosis
        JsonNode jsonNode = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        UUID diagnosisId = UUID.fromString(jsonNode.get("data").get("diagnosisId").asText());

        Diagnosis persistedDiagnosis = diagnosisRepository.findByIdAndDeletedAtIsNull(diagnosisId).orElseThrow();
        assertThat(persistedDiagnosis.getUser()).isNull();
        assertThat(persistedDiagnosis.getPlant().getPlantCode()).isEqualTo("POTATO");
        assertThat(persistedDiagnosis.getDominantDisease().getDiseaseCode()).isEqualTo("POTATO_LATE_BLIGHT");
        assertThat(persistedDiagnosis.getLatitude()).isEqualTo(11.0528);
        assertThat(persistedDiagnosis.getLongitude()).isEqualTo(106.6661);
        assertThat(persistedDiagnosis.getDeviceInfo()).isEqualTo("Guest Mobile Browser");
    }

    // =========================================================================
    // 2. Protected Endpoints Reject Unauthenticated Guest Access
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/diagnoses - Guest request without Bearer token returns 401 Unauthorized")
    void testGuestDiagnosis_GetHistory_WithoutBearerToken_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/diagnoses")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_001")));
    }

    @Test
    @DisplayName("GET /api/v1/diagnoses/{id} - Guest request without Bearer token returns 401 Unauthorized")
    void testGuestDiagnosis_GetDetail_WithoutBearerToken_Returns401Unauthorized() throws Exception {
        UUID anyDiagnosisId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/diagnoses/" + anyDiagnosisId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_001")));
    }

    @Test
    @DisplayName("DELETE /api/v1/diagnoses/{id} - Guest request without Bearer token returns 401 Unauthorized")
    void testGuestDiagnosis_DeleteDiagnosis_WithoutBearerToken_Returns401Unauthorized() throws Exception {
        UUID anyDiagnosisId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/diagnoses/" + anyDiagnosisId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_001")));
    }
}
