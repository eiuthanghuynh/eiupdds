package com.eiu.pdds.disease.controller;

import com.eiu.pdds.auth.security.JwtTokenProvider;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.dto.disease.DiseaseCreateRequest;
import com.eiu.pdds.dto.disease.DiseaseUpdateRequest;
import com.eiu.pdds.integration.roboflow.RoboflowClient;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.plant.repository.PlantRepository;
import com.eiu.pdds.storage.service.StorageService;
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

import java.util.List;
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
class DiseaseAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @BeforeEach
    void setUp() {
        User adminUser = createOrGetUser("disease_admin_user", "disease_admin@eiu.edu.vn", Role.ROLE_ADMIN);
        User expertUser = createOrGetUser("disease_expert_user", "disease_expert@eiu.edu.vn", Role.ROLE_EXPERT);
        User normalUser = createOrGetUser("disease_normal_farmer", "disease_farmer@eiu.edu.vn", Role.ROLE_USER);

        adminToken = jwtTokenProvider.generateAccessToken(adminUser);
        expertToken = jwtTokenProvider.generateAccessToken(expertUser);
        userToken = jwtTokenProvider.generateAccessToken(normalUser);

        seededLateBlight = diseaseRepository.findByDiseaseCode("POTATO_LATE_BLIGHT")
                .orElseGet(() -> diseaseRepository.save(Disease.builder()
                        .diseaseCode("POTATO_LATE_BLIGHT")
                        .commonName("Potato late blight")
                        .scientificName("Phytophthora infestans")
                        .severity(Severity.CRITICAL)
                        .symptoms("Dark brown to black water-soaked lesions.")
                        .description("Destructive fungal-like oomycete disease.")
                        .build()));

        seededPotato = plantRepository.findByPlantCode("POTATO")
                .orElseGet(() -> plantRepository.save(Plant.builder()
                        .plantCode("POTATO")
                        .commonName("Potato")
                        .scientificName("Solanum tuberosum")
                        .iconUrl("https://cdn.eiu.edu.vn/icons/potato.svg")
                        .description("Tuber crop")
                        .build()));
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

    // ==========================================
    // 1. GET /api/v1/diseases (Catalog & Pagination)
    // ==========================================

    @Test
    @DisplayName("GET /api/v1/diseases - Public access returns paginated PageResponse<DiseaseSummaryDto>")
    void testGetAllDiseases_PublicAccess_ReturnsPageResponse() throws Exception {
        mockMvc.perform(get("/api/v1/diseases")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()))
                .andExpect(jsonPath("$.data.content.length()", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.page", is(0)))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalPages", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.content[0].id", notNullValue()))
                .andExpect(jsonPath("$.data.content[0].diseaseCode", notNullValue()))
                .andExpect(jsonPath("$.data.content[0].commonName", notNullValue()))
                .andExpect(jsonPath("$.data.content[0].severity", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/diseases?keyword=late - Filters disease catalog by keyword matching name or code")
    void testGetAllDiseases_WithKeywordFilter_ReturnsFilteredDiseases() throws Exception {
        mockMvc.perform(get("/api/v1/diseases")
                        .param("keyword", "late")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()))
                .andExpect(jsonPath("$.data.content[*].diseaseCode", hasItem("POTATO_LATE_BLIGHT")))
                .andExpect(jsonPath("$.data.content[*].diseaseCode", not(hasItem("POTATO_EARLY_BLIGHT"))));
    }

    @Test
    @DisplayName("GET /api/v1/diseases?plantCode=POTATO - Filters disease catalog by associated host crop")
    void testGetAllDiseases_WithPlantFilter_ReturnsDiseasesForPlant() throws Exception {
        mockMvc.perform(get("/api/v1/diseases")
                        .param("plantCode", "POTATO")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[*].diseaseCode", hasItem("POTATO_LATE_BLIGHT")));
    }

    @Test
    @DisplayName("GET /api/v1/diseases?severity=CRITICAL - Filters disease catalog strictly by pathological severity")
    void testGetAllDiseases_WithSeverityFilter_ReturnsMatchingSeverity() throws Exception {
        mockMvc.perform(get("/api/v1/diseases")
                        .param("severity", "CRITICAL")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[*].severity", everyItem(is("CRITICAL"))));
    }

    @Test
    @DisplayName("GET /api/v1/diseases?unpaged=true - Returns entire disease catalog without pagination truncation")
    void testGetAllDiseases_WithUnpagedTrue_ReturnsAllWithoutTruncation() throws Exception {
        mockMvc.perform(get("/api/v1/diseases")
                        .param("unpaged", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()))
                .andExpect(jsonPath("$.data.content.length()", greaterThanOrEqualTo(2)));
    }

    // ==========================================
    // 2. GET /api/v1/diseases/{idOrCode} (Details)
    // ==========================================

    @Test
    @DisplayName("GET /api/v1/diseases/{idOrCode} - Successfully retrieves disease details by UUID primary key")
    void testGetDiseaseById_ExistingUuid_ReturnsDiseaseDetailWithTreatments() throws Exception {
        mockMvc.perform(get("/api/v1/diseases/" + seededLateBlight.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(seededLateBlight.getId().toString())))
                .andExpect(jsonPath("$.data.diseaseCode", is("POTATO_LATE_BLIGHT")))
                .andExpect(jsonPath("$.data.scientificName", is("Phytophthora infestans")))
                .andExpect(jsonPath("$.data.severity", is("CRITICAL")))
                .andExpect(jsonPath("$.data.treatments", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/diseases/{idOrCode} - Successfully retrieves disease details by unique diseaseCode string")
    void testGetDiseaseByCode_ExistingCode_ReturnsDiseaseDetail() throws Exception {
        mockMvc.perform(get("/api/v1/diseases/POTATO_LATE_BLIGHT")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.diseaseCode", is("POTATO_LATE_BLIGHT")))
                .andExpect(jsonPath("$.data.commonName", notNullValue()))
                .andExpect(jsonPath("$.data.treatments", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/diseases/{idOrCode} - Returns 404 NOT FOUND when UUID does not exist")
    void testGetDiseaseById_NonexistentUuid_Returns404NotFound() throws Exception {
        UUID randomUuid = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/diseases/" + randomUuid)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/v1/diseases/{idOrCode} - Returns 404 NOT FOUND when diseaseCode does not exist")
    void testGetDiseaseByCode_NonexistentCode_Returns404NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/diseases/UNKNOWN_DISEASE_CODE_XYZ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ==========================================
    // 3. POST /api/v1/diseases (Create)
    // ==========================================

    @Test
    @DisplayName("POST /api/v1/diseases - As ROLE_ADMIN returns 201 Created and persists disease")
    void testCreateDisease_AsAdmin_Returns201CreatedAndPersists() throws Exception {
        DiseaseCreateRequest request = new DiseaseCreateRequest(
                "CORN_COMMON_SMUT",
                "Common Corn Smut",
                "Ustilago maydis",
                Severity.MODERATE,
                "Prominent fleshy galls that develop on ears, tassels, stalks, and leaves.",
                "Fungal pathogen common in humid temperate maize growing areas.",
                List.of("CORN")
        );

        mockMvc.perform(post("/api/v1/diseases")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.diseaseCode", is("CORN_COMMON_SMUT")))
                .andExpect(jsonPath("$.data.commonName", is("Common Corn Smut")))
                .andExpect(jsonPath("$.data.severity", is("MODERATE")));

        assertThat(diseaseRepository.findByDiseaseCode("CORN_COMMON_SMUT")).isPresent();
    }

    @Test
    @DisplayName("POST /api/v1/diseases - As ROLE_EXPERT returns 201 Created")
    void testCreateDisease_AsExpert_Returns201Created() throws Exception {
        DiseaseCreateRequest request = new DiseaseCreateRequest(
                "APPLE_BLACK_ROT",
                "Apple Black Rot",
                "Botryosphaeria obtusa",
                Severity.HIGH,
                "Frog-eye leaf spots and decaying fruit rot.",
                "Fungal infection affecting apple trees.",
                List.of()
        );

        mockMvc.perform(post("/api/v1/diseases")
                        .header("Authorization", "Bearer " + expertToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.diseaseCode", is("APPLE_BLACK_ROT")));

        assertThat(diseaseRepository.findByDiseaseCode("APPLE_BLACK_ROT")).isPresent();
    }

    @Test
    @DisplayName("POST /api/v1/diseases - As ROLE_USER returns 403 Forbidden")
    void testCreateDisease_AsUser_Returns403Forbidden() throws Exception {
        DiseaseCreateRequest request = new DiseaseCreateRequest(
                "FORBIDDEN_DISEASE",
                "Unauthorized Disease",
                "Pathogen test",
                Severity.LOW,
                "Symptoms",
                "Description",
                List.of()
        );

        mockMvc.perform(post("/api/v1/diseases")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/diseases - Unauthenticated request returns 401 Unauthorized")
    void testCreateDisease_Unauthenticated_Returns401Unauthorized() throws Exception {
        DiseaseCreateRequest request = new DiseaseCreateRequest(
                "UNAUTH_DISEASE",
                "Unauth Disease",
                "Pathogen test",
                Severity.LOW,
                "Symptoms",
                "Description",
                List.of()
        );

        mockMvc.perform(post("/api/v1/diseases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/diseases - Validation failure on blank commonName returns 400 Bad Request")
    void testCreateDisease_ValidationFailure_BlankCommonName_Returns400BadRequest() throws Exception {
        DiseaseCreateRequest request = new DiseaseCreateRequest(
                "VALID_CODE",
                "", // Blank common name violates @NotBlank
                "Pathogen",
                Severity.LOW,
                "Symptoms",
                "Description",
                List.of()
        );

        mockMvc.perform(post("/api/v1/diseases")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/v1/diseases - Validation failure on lowercase diseaseCode violates regex returning 400 Bad Request")
    void testCreateDisease_ValidationFailure_InvalidDiseaseCode_Returns400BadRequest() throws Exception {
        DiseaseCreateRequest request = new DiseaseCreateRequest(
                "invalid_lowercase_code", // Lowercase violates ^[A-Z0-9_]{3,64}$
                "Valid Name",
                "Pathogen",
                Severity.LOW,
                "Symptoms",
                "Description",
                List.of()
        );

        mockMvc.perform(post("/api/v1/diseases")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ==========================================
    // 4. PUT /api/v1/diseases/{id} (Update)
    // ==========================================

    @Test
    @DisplayName("PUT /api/v1/diseases/{id} - As ROLE_ADMIN returns 200 OK and updates disease metadata")
    void testUpdateDisease_AsAdmin_Returns200OkAndUpdates() throws Exception {
        DiseaseUpdateRequest request = new DiseaseUpdateRequest(
                "Updated Potato Late Blight Common Name",
                "Phytophthora infestans (Updated)",
                Severity.CRITICAL,
                "Updated necrotic black foliage lesions.",
                "Updated pathological summary.",
                List.of("POTATO")
        );

        mockMvc.perform(put("/api/v1/diseases/" + seededLateBlight.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(seededLateBlight.getId().toString())))
                .andExpect(jsonPath("$.data.commonName", is("Updated Potato Late Blight Common Name")))
                .andExpect(jsonPath("$.data.scientificName", is("Phytophthora infestans (Updated)")));

        Disease updated = diseaseRepository.findById(seededLateBlight.getId()).orElseThrow();
        assertThat(updated.getCommonName()).isEqualTo("Updated Potato Late Blight Common Name");
    }

    @Test
    @DisplayName("PUT /api/v1/diseases/{id} - As ROLE_EXPERT returns 200 OK")
    void testUpdateDisease_AsExpert_Returns200Ok() throws Exception {
        DiseaseUpdateRequest request = new DiseaseUpdateRequest(
                "Expert Refined Late Blight Name",
                "Phytophthora infestans",
                Severity.CRITICAL,
                "Expert symptom description.",
                "Expert etiology.",
                List.of("POTATO")
        );

        mockMvc.perform(put("/api/v1/diseases/" + seededLateBlight.getId())
                        .header("Authorization", "Bearer " + expertToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.commonName", is("Expert Refined Late Blight Name")));
    }

    @Test
    @DisplayName("PUT /api/v1/diseases/{id} - As ROLE_USER returns 403 Forbidden")
    void testUpdateDisease_AsUser_Returns403Forbidden() throws Exception {
        DiseaseUpdateRequest request = new DiseaseUpdateRequest(
                "Hacked Disease Name",
                "Hacked",
                Severity.LOW,
                "Symptoms",
                "Description",
                List.of()
        );

        mockMvc.perform(put("/api/v1/diseases/" + seededLateBlight.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT /api/v1/diseases/{id} - Unauthenticated request returns 401 Unauthorized")
    void testUpdateDisease_Unauthenticated_Returns401Unauthorized() throws Exception {
        DiseaseUpdateRequest request = new DiseaseUpdateRequest(
                "Unauth Update",
                "Binomial",
                Severity.LOW,
                "Symptoms",
                "Description",
                List.of()
        );

        mockMvc.perform(put("/api/v1/diseases/" + seededLateBlight.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/v1/diseases/{id} - Returns 404 NOT FOUND for nonexistent disease ID")
    void testUpdateDisease_NonexistentId_Returns404NotFound() throws Exception {
        UUID nonexistentId = UUID.randomUUID();
        DiseaseUpdateRequest request = new DiseaseUpdateRequest(
                "Missing Disease Update",
                "Scientific name",
                Severity.LOW,
                "Symptoms",
                "Description",
                List.of()
        );

        mockMvc.perform(put("/api/v1/diseases/" + nonexistentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("PUT /api/v1/diseases/{id} - Validation failure on null severity returns 400 Bad Request")
    void testUpdateDisease_ValidationFailure_NullSeverity_Returns400BadRequest() throws Exception {
        DiseaseUpdateRequest request = new DiseaseUpdateRequest(
                "Valid Common Name",
                "Scientific Name",
                null, // Severity @NotNull violation
                "Symptoms",
                "Description",
                List.of()
        );

        mockMvc.perform(put("/api/v1/diseases/" + seededLateBlight.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    // ==========================================
    // 5. DELETE /api/v1/diseases/{id} (Soft Delete)
    // ==========================================

    @Test
    @DisplayName("DELETE /api/v1/diseases/{id} - As ROLE_USER returns 403 Forbidden")
    void testDeleteDisease_AsUser_Returns403Forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/diseases/" + seededLateBlight.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /api/v1/diseases/{id} - Unauthenticated request returns 401 Unauthorized")
    void testDeleteDisease_Unauthenticated_Returns401Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/diseases/" + seededLateBlight.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("DELETE /api/v1/diseases/{id} - As ROLE_ADMIN soft deletes disease; subsequent GET returns 404")
    void testDeleteDisease_AsAdmin_SoftDeletesAndHidesFromCatalog() throws Exception {
        Disease tempDisease = diseaseRepository.save(Disease.builder()
                .diseaseCode("TEMP_DELETE_TARGET_ADMIN")
                .commonName("Temporary Disease To Delete")
                .scientificName("Temp species")
                .severity(Severity.LOW)
                .build());

        UUID targetId = tempDisease.getId();

        // 1. Delete as ADMIN -> 200 OK
        mockMvc.perform(delete("/api/v1/diseases/" + targetId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 2. Verify soft-delete tombstone in database
        Disease inDb = diseaseRepository.findById(targetId).orElseThrow();
        assertThat(inDb.isDeleted()).isTrue();
        assertThat(inDb.getDeletedAt()).isNotNull();

        // 3. Subsequent GET by UUID -> 404 Not Found
        mockMvc.perform(get("/api/v1/diseases/" + targetId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));

        // 4. Subsequent GET by diseaseCode -> 404 Not Found
        mockMvc.perform(get("/api/v1/diseases/TEMP_DELETE_TARGET_ADMIN")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));

        // 5. Subsequent GET catalog excludes soft-deleted disease
        mockMvc.perform(get("/api/v1/diseases")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].diseaseCode", not(hasItem("TEMP_DELETE_TARGET_ADMIN"))));
    }

    @Test
    @DisplayName("DELETE /api/v1/diseases/{id} - As ROLE_EXPERT soft deletes disease")
    void testDeleteDisease_AsExpert_SoftDeletes() throws Exception {
        Disease tempDisease = diseaseRepository.save(Disease.builder()
                .diseaseCode("TEMP_DELETE_TARGET_EXPERT")
                .commonName("Expert Target Disease")
                .scientificName("Expert species")
                .severity(Severity.MODERATE)
                .build());

        UUID targetId = tempDisease.getId();

        mockMvc.perform(delete("/api/v1/diseases/" + targetId)
                        .header("Authorization", "Bearer " + expertToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        Disease inDb = diseaseRepository.findById(targetId).orElseThrow();
        assertThat(inDb.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("DELETE /api/v1/diseases/{id} - Returns 404 NOT FOUND for nonexistent ID")
    void testDeleteDisease_NonexistentId_Returns404NotFound() throws Exception {
        UUID nonexistentId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/diseases/" + nonexistentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
