package com.eiu.pdds;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class
})
class ApiContractTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/v1/diagnoses/analyze - Should return standardized dual coordinates, dominant disease, and treatments")
    void testAnalyzeEndpointContract() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "sample-potato-leaf.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[]{1, 2, 3, 4, 5, 6}
        );

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
