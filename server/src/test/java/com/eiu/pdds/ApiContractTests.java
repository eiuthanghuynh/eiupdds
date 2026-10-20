package com.eiu.pdds;

import com.eiu.pdds.integration.roboflow.RoboflowClient;
import com.eiu.pdds.integration.roboflow.dto.RoboflowWorkflowResponse;
import com.eiu.pdds.storage.service.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.List;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiContractTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoboflowClient roboflowClient;

    @MockBean
    private StorageService storageService;

    @BeforeEach
    void setUp() {
        Mockito.reset(storageService, roboflowClient);
    }

    @Test
    @DisplayName("POST /api/v1/diagnoses/analyze - Should return standardized dual coordinates, dominant disease, and treatments")
    void testAnalyzeEndpointContract() throws Exception {
        byte[] validJpegBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        };

        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "sample-potato-leaf.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                validJpegBytes
        );

        when(storageService.uploadImage(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn("https://s3.eiu.edu.vn/eiu-pdds-plants/potato-leaf-sample.jpg");

        RoboflowWorkflowResponse.ModelOutput mockOutput = new RoboflowWorkflowResponse.ModelOutput(
                new RoboflowWorkflowResponse.ImageInfo(399, 501),
                List.of(
                        new RoboflowWorkflowResponse.Prediction(322.0, 426.0, 154.0, 150.0, 0.7994398474693298, 13, "Potato leaf late blight", "31faab02-9bd3-47fe-8258-70e577b27006", "image"),
                        new RoboflowWorkflowResponse.Prediction(269.0, 128.0, 138.0, 254.0, 0.6795565485954285, 13, "Potato leaf late blight", "fefb09d5-fae1-49f9-b2b8-edddfb696a0d", "image"),
                        new RoboflowWorkflowResponse.Prediction(358.0, 142.5, 82.0, 255.0, 0.5705733895301819, 13, "Potato leaf late blight", "b70dd935-bc19-4044-b1cc-42b832671e6a", "image"),
                        new RoboflowWorkflowResponse.Prediction(99.0, 336.0, 198.0, 204.0, 0.5202842354774475, 12, "Potato leaf early blight", "ea11cf2c-127a-427b-8563-0b4b8f369ebc", "image"),
                        new RoboflowWorkflowResponse.Prediction(125.5, 226.5, 243.0, 245.0, 0.4390724301338196, 13, "Potato leaf late blight", "7066cc8e-d7e4-464e-9cff-91954cccb91d", "image")
                )
        );

        when(roboflowClient.inferPlantDisease(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.nullable(Double.class)))
                .thenReturn(mockOutput);

        mockMvc.perform(multipart("/api/v1/diagnoses/analyze")
                        .file(imageFile)
                        .param("plantCode", "POTATO")
                        .param("latitude", "11.0528")
                        .param("longitude", "106.6661")
                        .param("deviceInfo", "iPhone 15 Pro / iOS 17.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is("SUCCESS")))
                .andExpect(jsonPath("$.data.imageWidth", is(399)))
                .andExpect(jsonPath("$.data.imageHeight", is(501)))
                // Dominant Disease Check
                .andExpect(jsonPath("$.data.dominantDisease.diseaseCode", is("POTATO_LATE_BLIGHT")))
                .andExpect(jsonPath("$.data.dominantDisease.severity", is("CRITICAL")))
                .andExpect(jsonPath("$.data.dominantConfidence", closeTo(0.7994, 0.001)))
                // Bounding Boxes Check (5 detections from Roboflow model)
                .andExpect(jsonPath("$.data.boundingBoxes", hasSize(5)))
                // Check Detection 1 coordinates
                .andExpect(jsonPath("$.data.boundingBoxes[0].label", is("Potato leaf late blight")))
                .andExpect(jsonPath("$.data.boundingBoxes[0].x", is(322.0)))
                .andExpect(jsonPath("$.data.boundingBoxes[0].y", is(426.0)))
                .andExpect(jsonPath("$.data.boundingBoxes[0].width", is(154.0)))
                .andExpect(jsonPath("$.data.boundingBoxes[0].height", is(150.0)))
                .andExpect(jsonPath("$.data.boundingBoxes[0].xMin", is(245.0)))
                .andExpect(jsonPath("$.data.boundingBoxes[0].yMin", is(351.0)))
                // Check Normalized Top-Left Coordinates [0.0 - 1.0] for React Native Canvas/SVG
                .andExpect(jsonPath("$.data.boundingBoxes[0].xMinNormalized", closeTo(0.6140, 0.001)))
                .andExpect(jsonPath("$.data.boundingBoxes[0].yMinNormalized", closeTo(0.7006, 0.001)))
                .andExpect(jsonPath("$.data.boundingBoxes[0].widthNormalized", closeTo(0.3860, 0.001)))
                .andExpect(jsonPath("$.data.boundingBoxes[0].heightNormalized", closeTo(0.2994, 0.001)))
                // Co-occurring diseases (Co-infection)
                .andExpect(jsonPath("$.data.coOccurringDiseases", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.coOccurringDiseases[0].diseaseCode", is("POTATO_EARLY_BLIGHT")))
                // Curated Treatments
                .andExpect(jsonPath("$.data.recommendedTreatments", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.recommendedTreatments[0].title", notNullValue()))
                .andExpect(jsonPath("$.data.recommendedTreatments[0].instructions", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/diseases - Should return catalog of all supported diseases")
    void testGetAllDiseases() throws Exception {
        mockMvc.perform(get("/api/v1/diseases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.data[*].diseaseCode", hasItems("POTATO_LATE_BLIGHT", "POTATO_EARLY_BLIGHT", "TOMATO_LATE_BLIGHT")));
    }

    @Test
    @DisplayName("GET /api/v1/diseases/{code} - Should return disease details with treatment protocols")
    void testGetDiseaseByCode() throws Exception {
        mockMvc.perform(get("/api/v1/diseases/POTATO_LATE_BLIGHT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.diseaseCode", is("POTATO_LATE_BLIGHT")))
                .andExpect(jsonPath("$.data.severity", is("CRITICAL")))
                .andExpect(jsonPath("$.data.treatments", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("GET /api/v1/plants - Should return supported crop list")
    void testGetAllPlants() throws Exception {
        mockMvc.perform(get("/api/v1/plants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(3))))
                .andExpect(jsonPath("$.data[*].plantCode", hasItems("POTATO", "TOMATO", "CORN")));
    }

    @Test
    @DisplayName("GET /api/v1/plants/{code} - Should return crop details by plant code")
    void testGetPlantByCode() throws Exception {
        mockMvc.perform(get("/api/v1/plants/POTATO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.plantCode", is("POTATO")))
                .andExpect(jsonPath("$.data.commonName", containsString("Potato")));
    }
}
