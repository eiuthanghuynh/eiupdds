package com.eiu.pdds.treatment.controller;

import com.eiu.pdds.auth.security.JwtTokenProvider;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.dto.treatment.TreatmentCreateRequest;
import com.eiu.pdds.dto.treatment.TreatmentUpdateRequest;
import com.eiu.pdds.integration.roboflow.RoboflowClient;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.plant.repository.PlantRepository;
import com.eiu.pdds.storage.service.StorageService;
import com.eiu.pdds.treatment.entity.Treatment;
import com.eiu.pdds.treatment.entity.TreatmentType;
import com.eiu.pdds.treatment.repository.TreatmentRepository;
import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TreatmentAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private DiseaseRepository diseaseRepository;

    @Autowired
    private PlantRepository plantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RoboflowClient roboflowClient;

    @MockBean
    private StorageService storageService;

    private String adminToken;
    private String expertToken;
    private String userToken;

    private Disease seededLateBlight;
    private Plant seededPotato;
    private Treatment seededChemicalTreatment;

    @BeforeEach
    void setUp() {
        User adminUser = createOrGetUser("treatment_admin_user", "treatment_admin@eiu.edu.vn", Role.ROLE_ADMIN);
        User expertUser = createOrGetUser("treatment_expert_user", "treatment_expert@eiu.edu.vn", Role.ROLE_EXPERT);
        User normalUser = createOrGetUser("treatment_normal_farmer", "treatment_farmer@eiu.edu.vn", Role.ROLE_USER);

        adminToken = jwtTokenProvider.generateAccessToken(adminUser);
        expertToken = jwtTokenProvider.generateAccessToken(expertUser);
        userToken = jwtTokenProvider.generateAccessToken(normalUser);

        seededPotato = plantRepository.findByPlantCode("POTATO")
                .orElseGet(() -> plantRepository.save(Plant.builder()
                        .plantCode("POTATO")
                        .commonName("Potato")
                        .scientificName("Solanum tuberosum")
                        .build()));

        seededLateBlight = diseaseRepository.findByDiseaseCode("POTATO_LATE_BLIGHT")
                .orElseGet(() -> diseaseRepository.save(Disease.builder()
                        .diseaseCode("POTATO_LATE_BLIGHT")
                        .commonName("Potato late blight")
                        .scientificName("Phytophthora infestans")
                        .severity(Severity.CRITICAL)
                        .build()));

        seededChemicalTreatment = treatmentRepository.save(Treatment.builder()
                .disease(seededLateBlight)
                .plant(seededPotato)
                .treatmentType(TreatmentType.CHEMICAL)
                .title("Metalaxyl + Mancozeb Systemic Fungicide Spray")
                .instructions("Spray thoroughly on foliage.")
                .activeIngredients("Metalaxyl 8% + Mancozeb 64%")
                .dosageRecommendation("50g per 16L sprayer")
                .safetyIntervalDays(7)
                .warning("Limit to 3 applications per season.")
                .build());
    }

    private User createOrGetUser(String username, String email, Role role) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(User.builder()
                        .username(username)
                        .email(email)
                        .passwordHash("hashed_password_for_tests")
                        .role(role)
                        .isActive(true)
                        .build()));
    }

    // ========================================================
    // 1. GET /api/v1/diseases/{diseaseId}/treatments (Query)
    // ========================================================

    @Test
    @DisplayName("GET /api/v1/diseases/{diseaseId}/treatments - Public access returns list of treatments for given disease")
    void testGetTreatmentsByDiseaseId_PublicAccess_ReturnsTreatmentList() throws Exception {
        mockMvc.perform(get("/api/v1/diseases/" + seededLateBlight.getId() + "/treatments")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].id", notNullValue()))
                .andExpect(jsonPath("$.data[0].title", notNullValue()))
                .andExpect(jsonPath("$.data[0].treatmentType", notNullValue()))
                .andExpect(jsonPath("$.data[0].instructions", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/diseases/{diseaseId}/treatments?treatmentType=CHEMICAL - Filters treatments by category")
    void testGetTreatmentsByDiseaseId_WithTreatmentTypeFilter_ReturnsFilteredTreatments() throws Exception {
        // Also add a BIOLOGICAL treatment to test filtering discrimination
        treatmentRepository.save(Treatment.builder()
                .disease(seededLateBlight)
                .treatmentType(TreatmentType.BIOLOGICAL)
                .title("Bacillus Subtilis Biopesticide")
                .instructions("Apply as preventative spray.")
                .build());

        mockMvc.perform(get("/api/v1/diseases/" + seededLateBlight.getId() + "/treatments")
                        .param("treatmentType", "CHEMICAL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[*].treatmentType", everyItem(is("CHEMICAL"))));
    }

    @Test
    @DisplayName("GET /api/v1/diseases/{diseaseId}/treatments - Returns 404 NOT FOUND when diseaseId does not exist")
    void testGetTreatmentsByDiseaseId_NonexistentDisease_Returns404NotFound() throws Exception {
        UUID nonexistentDiseaseId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/diseases/" + nonexistentDiseaseId + "/treatments")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ========================================================
    // 2. POST /api/v1/diseases/{diseaseId}/treatments (Create)
    // ========================================================

    @Test
    @DisplayName("POST /api/v1/diseases/{diseaseId}/treatments - As ROLE_ADMIN returns 201 Created and persists treatment")
    void testCreateTreatment_AsAdmin_Returns201Created() throws Exception {
        TreatmentCreateRequest request = new TreatmentCreateRequest(
                TreatmentType.BIOLOGICAL,
                seededPotato.getId(),
                "Copper Octanoate Organic Bio-spray",
                "Apply thoroughly at first sign of disease symptoms.",
                "Copper Octanoate 10%",
                "30-40ml per 16L water",
                1,
                "Safe for beneficial organisms. Avoid spraying in intense heat."
        );

        mockMvc.perform(post("/api/v1/diseases/" + seededLateBlight.getId() + "/treatments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.treatmentType", is("BIOLOGICAL")))
                .andExpect(jsonPath("$.data.title", is("Copper Octanoate Organic Bio-spray")))
                .andExpect(jsonPath("$.data.safetyIntervalDays", is(1)));

        assertThat(treatmentRepository.findByDiseaseIdAndDeletedAtIsNull(seededLateBlight.getId()))
                .anyMatch(t -> t.getTitle().equals("Copper Octanoate Organic Bio-spray"));
    }

    @Test
    @DisplayName("POST /api/v1/diseases/{diseaseId}/treatments - As ROLE_EXPERT returns 201 Created")
    void testCreateTreatment_AsExpert_Returns201Created() throws Exception {
        TreatmentCreateRequest request = new TreatmentCreateRequest(
                TreatmentType.PREVENTIVE,
                null,
                "Drip Irrigation and Mulching Practice",
                "Use drip lines to avoid wetting leaf foliage.",
                null,
                "Standard field spacing",
                0,
                "Keep plastic mulch clean of soil splashes."
        );

        mockMvc.perform(post("/api/v1/diseases/" + seededLateBlight.getId() + "/treatments")
                        .header("Authorization", "Bearer " + expertToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.treatmentType", is("PREVENTIVE")))
                .andExpect(jsonPath("$.data.title", is("Drip Irrigation and Mulching Practice")));
    }

    @Test
    @DisplayName("POST /api/v1/diseases/{diseaseId}/treatments - As ROLE_USER returns 403 Forbidden")
    void testCreateTreatment_AsUser_Returns403Forbidden() throws Exception {
        TreatmentCreateRequest request = new TreatmentCreateRequest(
                TreatmentType.CHEMICAL,
                null,
                "Unauthorized Treatment",
                "Instructions",
                null,
                null,
                0,
                null
        );

        mockMvc.perform(post("/api/v1/diseases/" + seededLateBlight.getId() + "/treatments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/diseases/{diseaseId}/treatments - Unauthenticated request returns 401 Unauthorized")
    void testCreateTreatment_Unauthenticated_Returns401Unauthorized() throws Exception {
        TreatmentCreateRequest request = new TreatmentCreateRequest(
                TreatmentType.CHEMICAL,
                null,
                "Unauth Treatment",
                "Instructions",
                null,
                null,
                0,
                null
        );

        mockMvc.perform(post("/api/v1/diseases/" + seededLateBlight.getId() + "/treatments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/diseases/{diseaseId}/treatments - Nonexistent diseaseId returns 404 NOT FOUND")
    void testCreateTreatment_NonexistentDisease_Returns404NotFound() throws Exception {
        UUID nonexistentDiseaseId = UUID.randomUUID();
        TreatmentCreateRequest request = new TreatmentCreateRequest(
                TreatmentType.CHEMICAL,
                null,
                "Valid Title",
                "Valid Instructions",
                null,
                null,
                0,
                null
        );

        mockMvc.perform(post("/api/v1/diseases/" + nonexistentDiseaseId + "/treatments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/diseases/{diseaseId}/treatments - Validation failure on blank title returns 400 Bad Request")
    void testCreateTreatment_ValidationFailure_BlankTitle_Returns400BadRequest() throws Exception {
        TreatmentCreateRequest request = new TreatmentCreateRequest(
                TreatmentType.CHEMICAL,
                null,
                "", // Blank title violates @NotBlank
                "Valid instructions",
                null,
                null,
                0,
                null
        );

        mockMvc.perform(post("/api/v1/diseases/" + seededLateBlight.getId() + "/treatments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/diseases/{diseaseId}/treatments - Validation failure on negative safetyIntervalDays returns 400 Bad Request")
    void testCreateTreatment_ValidationFailure_NegativeSafetyInterval_Returns400BadRequest() throws Exception {
        TreatmentCreateRequest request = new TreatmentCreateRequest(
                TreatmentType.CHEMICAL,
                null,
                "Valid Title",
                "Valid instructions",
                null,
                null,
                -5, // Negative violates @PositiveOrZero
                null
        );

        mockMvc.perform(post("/api/v1/diseases/" + seededLateBlight.getId() + "/treatments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ========================================================
    // 3. PUT /api/v1/treatments/{treatmentId} (Update)
    // ========================================================

    @Test
    @DisplayName("PUT /api/v1/treatments/{treatmentId} - As ROLE_ADMIN returns 200 OK and updates treatment")
    void testUpdateTreatment_AsAdmin_Returns200OkAndUpdates() throws Exception {
        TreatmentUpdateRequest request = new TreatmentUpdateRequest(
                TreatmentType.CHEMICAL,
                seededPotato.getId(),
                "Updated Metalaxyl-M Systemic Spray",
                "Updated comprehensive spray instructions across canopy.",
                "Mefenoxam 4% + Mancozeb 64%",
                "60g per 16L sprayer",
                10,
                "Rotate with non-phenylamide fungicides."
        );

        mockMvc.perform(put("/api/v1/treatments/" + seededChemicalTreatment.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(seededChemicalTreatment.getId().toString())))
                .andExpect(jsonPath("$.data.title", is("Updated Metalaxyl-M Systemic Spray")))
                .andExpect(jsonPath("$.data.safetyIntervalDays", is(10)));

        Treatment updated = treatmentRepository.findById(seededChemicalTreatment.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("Updated Metalaxyl-M Systemic Spray");
        assertThat(updated.getSafetyIntervalDays()).isEqualTo(10);
    }

    @Test
    @DisplayName("PUT /api/v1/treatments/{treatmentId} - As ROLE_EXPERT returns 200 OK")
    void testUpdateTreatment_AsExpert_Returns200Ok() throws Exception {
        TreatmentUpdateRequest request = new TreatmentUpdateRequest(
                TreatmentType.CHEMICAL,
                seededPotato.getId(),
                "Expert Reviewed Chemical Protocol",
                "Expert approved application timing at dusk.",
                "Metalaxyl 8%",
                "40g per 16L",
                7,
                "Wear protective PPE gear."
        );

        mockMvc.perform(put("/api/v1/treatments/" + seededChemicalTreatment.getId())
                        .header("Authorization", "Bearer " + expertToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Expert Reviewed Chemical Protocol")));
    }

    @Test
    @DisplayName("PUT /api/v1/treatments/{treatmentId} - As ROLE_USER returns 403 Forbidden")
    void testUpdateTreatment_AsUser_Returns403Forbidden() throws Exception {
        TreatmentUpdateRequest request = new TreatmentUpdateRequest(
                TreatmentType.CHEMICAL,
                null,
                "Unauthorized Edit",
                "Instructions",
                null,
                null,
                0,
                null
        );

        mockMvc.perform(put("/api/v1/treatments/" + seededChemicalTreatment.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/treatments/{treatmentId} - Unauthenticated request returns 401 Unauthorized")
    void testUpdateTreatment_Unauthenticated_Returns401Unauthorized() throws Exception {
        TreatmentUpdateRequest request = new TreatmentUpdateRequest(
                TreatmentType.CHEMICAL,
                null,
                "Unauth Edit",
                "Instructions",
                null,
                null,
                0,
                null
        );

        mockMvc.perform(put("/api/v1/treatments/" + seededChemicalTreatment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/v1/treatments/{treatmentId} - Returns 404 NOT FOUND for nonexistent treatmentId")
    void testUpdateTreatment_NonexistentTreatment_Returns404NotFound() throws Exception {
        UUID nonexistentTreatmentId = UUID.randomUUID();
        TreatmentUpdateRequest request = new TreatmentUpdateRequest(
                TreatmentType.CHEMICAL,
                null,
                "Valid Title",
                "Valid Instructions",
                null,
                null,
                0,
                null
        );

        mockMvc.perform(put("/api/v1/treatments/" + nonexistentTreatmentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("PUT /api/v1/treatments/{treatmentId} - Validation failure on blank instructions returns 400 Bad Request")
    void testUpdateTreatment_ValidationFailure_BlankInstructions_Returns400BadRequest() throws Exception {
        TreatmentUpdateRequest request = new TreatmentUpdateRequest(
                TreatmentType.CHEMICAL,
                null,
                "Valid Title",
                "", // Blank instructions violates @NotBlank
                null,
                null,
                0,
                null
        );

        mockMvc.perform(put("/api/v1/treatments/" + seededChemicalTreatment.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ========================================================
    // 4. DELETE /api/v1/treatments/{treatmentId} (Soft Delete)
    // ========================================================

    @Test
    @DisplayName("DELETE /api/v1/treatments/{treatmentId} - As ROLE_USER returns 403 Forbidden")
    void testDeleteTreatment_AsUser_Returns403Forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/treatments/" + seededChemicalTreatment.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/treatments/{treatmentId} - Unauthenticated request returns 401 Unauthorized")
    void testDeleteTreatment_Unauthenticated_Returns401Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/treatments/" + seededChemicalTreatment.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/v1/treatments/{treatmentId} - As ROLE_ADMIN soft deletes treatment; excluded from subsequent queries")
    void testDeleteTreatment_AsAdmin_SoftDeletesTreatment() throws Exception {
        Treatment tempTreatment = treatmentRepository.save(Treatment.builder()
                .disease(seededLateBlight)
                .treatmentType(TreatmentType.CULTURAL)
                .title("Temporary Cultural Technique To Delete")
                .instructions("Remove infected bottom leaves.")
                .build());

        UUID targetId = tempTreatment.getId();

        // 1. Delete as ADMIN -> 200 OK
        mockMvc.perform(delete("/api/v1/treatments/" + targetId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 2. Verify soft-delete in database
        Treatment inDb = treatmentRepository.findById(targetId).orElseThrow();
        assertThat(inDb.isDeleted()).isTrue();
        assertThat(inDb.getDeletedAt()).isNotNull();

        // 3. Verify treatment list for disease excludes this soft-deleted treatment
        mockMvc.perform(get("/api/v1/diseases/" + seededLateBlight.getId() + "/treatments")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", not(hasItem(targetId.toString()))));
    }

    @Test
    @DisplayName("DELETE /api/v1/treatments/{treatmentId} - As ROLE_EXPERT soft deletes treatment")
    void testDeleteTreatment_AsExpert_SoftDeletesTreatment() throws Exception {
        Treatment tempTreatment = treatmentRepository.save(Treatment.builder()
                .disease(seededLateBlight)
                .treatmentType(TreatmentType.CULTURAL)
                .title("Expert Targeted Cultural Technique")
                .instructions("Clear debris.")
                .build());

        UUID targetId = tempTreatment.getId();

        mockMvc.perform(delete("/api/v1/treatments/" + targetId)
                        .header("Authorization", "Bearer " + expertToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        Treatment inDb = treatmentRepository.findById(targetId).orElseThrow();
        assertThat(inDb.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("DELETE /api/v1/treatments/{treatmentId} - Returns 404 NOT FOUND for nonexistent treatmentId")
    void testDeleteTreatment_NonexistentTreatment_Returns404NotFound() throws Exception {
        UUID nonexistentTreatmentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/treatments/" + nonexistentTreatmentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
