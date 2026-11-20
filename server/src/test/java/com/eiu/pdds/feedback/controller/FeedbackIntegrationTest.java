package com.eiu.pdds.feedback.controller;

import com.eiu.pdds.audit.event.AdminAuditEvent;
import com.eiu.pdds.auth.security.JwtTokenProvider;
import com.eiu.pdds.diagnosis.entity.BoundingBox;
import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.entity.DiagnosisStatus;
import com.eiu.pdds.diagnosis.repository.BoundingBoxRepository;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.feedback.entity.DiagnosisFeedback;
import com.eiu.pdds.feedback.repository.DiagnosisFeedbackRepository;
import com.eiu.pdds.integration.roboflow.RoboflowClient;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.plant.repository.PlantRepository;
import com.eiu.pdds.storage.service.StorageService;
import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RecordApplicationEvents
class FeedbackIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApplicationEvents applicationEvents;

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
    private DiagnosisFeedbackRepository feedbackRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RoboflowClient roboflowClient;

    @MockBean
    private StorageService storageService;

    private User farmerA;
    private User farmerB;
    private User expertUser;
    private User adminUser;

    private String farmerAToken;
    private String farmerBToken;
    private String expertToken;
    private String adminToken;

    private Plant potatoPlant;
    private Plant tomatoPlant;
    private Disease lateBlightDisease;
    private Disease earlyBlightDisease;

    private Diagnosis farmerADiagnosis;
    private Diagnosis farmerBDiagnosis;
    private Diagnosis guestDiagnosis;
    private Diagnosis softDeletedDiagnosis;

    @BeforeEach
    void setUp() {
        feedbackRepository.deleteAll();
        boundingBoxRepository.deleteAll();
        diagnosisRepository.deleteAll();

        farmerA = createOrGetUser("fb_farmer_a", "fb_farmer_a@farm.vn", Role.ROLE_USER);
        farmerB = createOrGetUser("fb_farmer_b", "fb_farmer_b@farm.vn", Role.ROLE_USER);
        expertUser = createOrGetUser("fb_expert", "fb_expert@eiu.edu.vn", Role.ROLE_EXPERT);
        adminUser = createOrGetUser("fb_admin", "fb_admin@eiu.edu.vn", Role.ROLE_ADMIN);

        farmerAToken = jwtTokenProvider.generateAccessToken(farmerA);
        farmerBToken = jwtTokenProvider.generateAccessToken(farmerB);
        expertToken = jwtTokenProvider.generateAccessToken(expertUser);
        adminToken = jwtTokenProvider.generateAccessToken(adminUser);

        potatoPlant = plantRepository.findByPlantCode("POTATO").orElseThrow();
        tomatoPlant = plantRepository.findByPlantCode("TOMATO").orElseThrow();
        lateBlightDisease = diseaseRepository.findByDiseaseCode("POTATO_LATE_BLIGHT").orElseThrow();
        earlyBlightDisease = diseaseRepository.findByDiseaseCode("POTATO_EARLY_BLIGHT").orElseThrow();

        farmerADiagnosis = createDiagnosis(farmerA, potatoPlant, lateBlightDisease, "https://s3.eiu.edu.vn/a.jpg", false);
        farmerBDiagnosis = createDiagnosis(farmerB, tomatoPlant, lateBlightDisease, "https://s3.eiu.edu.vn/b.jpg", false);
        guestDiagnosis = createDiagnosis(null, potatoPlant, lateBlightDisease, "https://s3.eiu.edu.vn/guest.jpg", false);
        softDeletedDiagnosis = createDiagnosis(farmerA, potatoPlant, lateBlightDisease, "https://s3.eiu.edu.vn/deleted.jpg", true);
    }

    private User createOrGetUser(String username, String email, Role role) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(User.builder()
                        .username(username)
                        .email(email)
                        .passwordHash("hashed_password")
                        .fullName(username)
                        .role(role)
                        .isActive(true)
                        .build()));
    }

    private Diagnosis createDiagnosis(User owner, Plant plant, Disease dominantDisease, String imageUrl, boolean deleted) {
        Diagnosis diag = Diagnosis.builder()
                .user(owner)
                .plant(plant)
                .originalImageUrl(imageUrl)
                .imageWidth(800)
                .imageHeight(600)
                .dominantDisease(dominantDisease)
                .dominantConfidence(dominantDisease != null ? 0.87 : null)
                .inferenceTimeMs(120)
                .rawAiResponse("{}")
                .status(DiagnosisStatus.COMPLETED)
                .build();
        if (deleted) {
            diag.markDeleted();
        }
        Diagnosis saved = diagnosisRepository.save(diag);

        BoundingBox box = BoundingBox.builder()
                .diagnosis(saved)
                .disease(dominantDisease)
                .label(dominantDisease != null ? dominantDisease.getCommonName() : "Potato healthy leaf")
                .confidence(0.87)
                .x(300.0)
                .y(220.0)
                .width(120.0)
                .height(80.0)
                .build();
        boundingBoxRepository.save(box);
        return saved;
    }

    // =========================================================================
    // 1. User Feedback Endpoints (/api/v1/diagnoses/{diagnosisId}/feedback)
    // =========================================================================

    @Test
    @DisplayName("POST & GET feedback - Unauthenticated requests return 401 AUTH_001")
    void userFeedback_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(post("/api/v1/diagnoses/{id}/feedback", farmerADiagnosis.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isAccurate\":true}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_001")));

        mockMvc.perform(get("/api/v1/diagnoses/{id}/feedback", farmerADiagnosis.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_001")));
    }

    @Test
    @DisplayName("POST feedback - IDOR, guest, and soft-deleted diagnoses return 404 DIAGNOSIS_001")
    void submitFeedback_IdorGuestAndDeleted_Return404() throws Exception {
        String body = "{\"isAccurate\":true}";

        // Another user's diagnosis
        mockMvc.perform(post("/api/v1/diagnoses/{id}/feedback", farmerBDiagnosis.getId())
                        .header("Authorization", "Bearer " + farmerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DIAGNOSIS_001")));

        // Guest diagnosis (user_id is null)
        mockMvc.perform(post("/api/v1/diagnoses/{id}/feedback", guestDiagnosis.getId())
                        .header("Authorization", "Bearer " + farmerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DIAGNOSIS_001")));

        // Soft-deleted diagnosis
        mockMvc.perform(post("/api/v1/diagnoses/{id}/feedback", softDeletedDiagnosis.getId())
                        .header("Authorization", "Bearer " + farmerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DIAGNOSIS_001")));
    }

    @Test
    @DisplayName("POST feedback - Upsert lifecycle 201 Created -> 200 OK -> 409 RES_002 after expert verification")
    void submitFeedback_UpsertLifecycleAndVerifiedImmutability() throws Exception {
        String createBody = """
                {
                  "isAccurate": false,
                  "suggestedDiseaseCode": "potato_early_blight",
                  "userComment": "  Brown target-like spots  "
                }
                """;

        mockMvc.perform(post("/api/v1/diagnoses/{id}/feedback", farmerADiagnosis.getId())
                        .header("Authorization", "Bearer " + farmerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.isAccurate", is(false)))
                .andExpect(jsonPath("$.data.suggestedDiseaseCode", is("POTATO_EARLY_BLIGHT")))
                .andExpect(jsonPath("$.data.userComment", is("Brown target-like spots")))
                .andExpect(jsonPath("$.data.expertVerified", is(false)));

        // GET my feedback
        mockMvc.perform(get("/api/v1/diagnoses/{id}/feedback", farmerADiagnosis.getId())
                        .header("Authorization", "Bearer " + farmerAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.suggestedDiseaseCode", is("POTATO_EARLY_BLIGHT")));

        // Update unverified feedback -> 200 OK
        String updateBody = """
                {
                  "isAccurate": false,
                  "suggestedDiseaseId": "%s",
                  "suggestedDiseaseCode": "POTATO_EARLY_BLIGHT",
                  "userComment": "Updated observation"
                }
                """.formatted(earlyBlightDisease.getId());

        MvcResult updateResult = mockMvc.perform(post("/api/v1/diagnoses/{id}/feedback", farmerADiagnosis.getId())
                        .header("Authorization", "Bearer " + farmerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userComment", is("Updated observation")))
                .andReturn();

        String feedbackId = objectMapper.readTree(updateResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        assertThat(feedbackRepository.findByDiagnosisIdAndDeletedAtIsNull(farmerADiagnosis.getId())).hasSize(1);

        // Expert verifies feedback
        mockMvc.perform(patch("/api/v1/admin/feedbacks/{id}/verify", feedbackId)
                        .header("Authorization", "Bearer " + expertToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expertNotes\":\"Verified by agronomist\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expertVerified", is(true)));

        // Farmer tries to modify verified feedback -> 409 RES_002
        mockMvc.perform(post("/api/v1/diagnoses/{id}/feedback", farmerADiagnosis.getId())
                        .header("Authorization", "Bearer " + farmerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isAccurate\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("RES_002")));
    }

    @Test
    @DisplayName("POST feedback - Validation edge cases (isAccurate=true+disease 400, mismatch 400, unknown disease 404, comment>2000 400)")
    void submitFeedback_ValidationAndErrorCases() throws Exception {
        // isAccurate = true with suggestedDiseaseCode -> 400 REQ_001
        mockMvc.perform(post("/api/v1/diagnoses/{id}/feedback", farmerADiagnosis.getId())
                        .header("Authorization", "Bearer " + farmerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isAccurate\":true,\"suggestedDiseaseCode\":\"POTATO_EARLY_BLIGHT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("REQ_001")));

        // Mismatched suggestedDiseaseId and suggestedDiseaseCode -> 400 REQ_001
        String mismatchBody = """
                {
                  "isAccurate": false,
                  "suggestedDiseaseId": "%s",
                  "suggestedDiseaseCode": "POTATO_LATE_BLIGHT"
                }
                """.formatted(earlyBlightDisease.getId());
        mockMvc.perform(post("/api/v1/diagnoses/{id}/feedback", farmerADiagnosis.getId())
                        .header("Authorization", "Bearer " + farmerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mismatchBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("REQ_001")));

        // Unknown disease -> 404 DISEASE_001
        mockMvc.perform(post("/api/v1/diagnoses/{id}/feedback", farmerADiagnosis.getId())
                        .header("Authorization", "Bearer " + farmerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isAccurate\":false,\"suggestedDiseaseCode\":\"NON_EXISTENT_999\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("DISEASE_001")));

        // userComment > 2000 chars -> 400 REQ_001
        String longComment = "a".repeat(2001);
        mockMvc.perform(post("/api/v1/diagnoses/{id}/feedback", farmerADiagnosis.getId())
                        .header("Authorization", "Bearer " + farmerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isAccurate\":true,\"userComment\":\"" + longComment + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("REQ_001")));

        // GET feedback when none submitted -> 404 FEEDBACK_001
        mockMvc.perform(get("/api/v1/diagnoses/{id}/feedback", farmerADiagnosis.getId())
                        .header("Authorization", "Bearer " + farmerAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("FEEDBACK_001")));
    }

    // =========================================================================
    // 2. Admin/Expert Feedback Moderation & Active Learning Export
    // =========================================================================

    @Test
    @DisplayName("Admin feedback RBAC - 401 unauth, 403 ROLE_USER, 200 ROLE_EXPERT & ROLE_ADMIN (while /admin/users remains 403 for EXPERT)")
    void adminFeedback_RbacChecks() throws Exception {
        mockMvc.perform(get("/api/v1/admin/feedbacks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_001")));

        mockMvc.perform(get("/api/v1/admin/feedbacks")
                        .header("Authorization", "Bearer " + farmerAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("AUTH_003")));

        mockMvc.perform(get("/api/v1/admin/feedbacks")
                        .header("Authorization", "Bearer " + expertToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        mockMvc.perform(get("/api/v1/admin/feedbacks")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Regression guard: /api/v1/admin/users/** still rejects ROLE_EXPERT
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + expertToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("AUTH_003")));
    }

    @Test
    @DisplayName("GET /api/v1/admin/feedbacks - Filters queue, validates date range & sort property, hides soft-deleted records")
    void getFeedbackQueue_FilteringAndValidation() throws Exception {
        DiagnosisFeedback fbPotato = feedbackRepository.save(DiagnosisFeedback.builder()
                .diagnosis(farmerADiagnosis)
                .user(farmerA)
                .isAccurate(false)
                .suggestedDisease(earlyBlightDisease)
                .expertVerified(false)
                .build());

        DiagnosisFeedback fbTomato = feedbackRepository.save(DiagnosisFeedback.builder()
                .diagnosis(farmerBDiagnosis)
                .user(farmerB)
                .isAccurate(true)
                .expertVerified(true)
                .verifiedByUser(expertUser)
                .verifiedAt(Instant.now())
                .build());

        DiagnosisFeedback fbOnDeletedDiag = feedbackRepository.save(DiagnosisFeedback.builder()
                .diagnosis(softDeletedDiagnosis)
                .user(farmerA)
                .isAccurate(true)
                .expertVerified(false)
                .build());

        DiagnosisFeedback softDeletedFb = DiagnosisFeedback.builder()
                .diagnosis(farmerADiagnosis)
                .user(farmerB)
                .isAccurate(true)
                .expertVerified(false)
                .build();
        softDeletedFb.markDeleted();
        feedbackRepository.save(softDeletedFb);

        // Filter by plantCode + expertVerified=false -> only fbPotato (not fbTomato, not deleted diag, not deleted fb)
        mockMvc.perform(get("/api/v1/admin/feedbacks")
                        .header("Authorization", "Bearer " + expertToken)
                        .param("plantCode", "potato")
                        .param("expertVerified", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id", is(fbPotato.getId().toString())))
                .andExpect(jsonPath("$.data.content[0].plantCode", is("POTATO")));

        // Verify fbTomato is visible when filtering expertVerified=true & isAccurate=true
        mockMvc.perform(get("/api/v1/admin/feedbacks")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("expertVerified", "true")
                        .param("isAccurate", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id", is(fbTomato.getId().toString())));

        // Invalid date range (fromDate > toDate) -> 400 REQ_001
        mockMvc.perform(get("/api/v1/admin/feedbacks")
                        .header("Authorization", "Bearer " + expertToken)
                        .param("fromDate", "2026-12-02T00:00:00Z")
                        .param("toDate", "2026-12-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("REQ_001")));

        // Invalid sort property -> 400 REQ_001
        mockMvc.perform(get("/api/v1/admin/feedbacks")
                        .header("Authorization", "Bearer " + expertToken)
                        .param("sort", "nonExistentColumn"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("REQ_001")));

        // Valid date range includes recent active feedbacks
        mockMvc.perform(get("/api/v1/admin/feedbacks")
                        .header("Authorization", "Bearer " + expertToken)
                        .param("fromDate", Instant.now().minusSeconds(60).toString())
                        .param("toDate", Instant.now().plusSeconds(60).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)));

        // Future date range excludes all existing feedbacks
        mockMvc.perform(get("/api/v1/admin/feedbacks")
                        .header("Authorization", "Bearer " + expertToken)
                        .param("fromDate", Instant.now().plusSeconds(3600).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        assertThat(fbOnDeletedDiag.getId()).isNotNull();
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/feedbacks/{id}/verify - Supports empty body, clears suggestion on isAccurate=true, publishes AdminAuditEvent, 404 on missing")
    void verifyFeedback_LifecycleAndAuditEvent() throws Exception {
        DiagnosisFeedback pending = feedbackRepository.save(DiagnosisFeedback.builder()
                .diagnosis(farmerADiagnosis)
                .user(farmerA)
                .isAccurate(false)
                .suggestedDisease(earlyBlightDisease)
                .expertVerified(false)
                .build());

        // Missing feedback -> 404 FEEDBACK_001
        mockMvc.perform(patch("/api/v1/admin/feedbacks/{id}/verify", UUID.randomUUID())
                        .header("Authorization", "Bearer " + expertToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("FEEDBACK_001")));

        // Rejects isAccurate=true combined with suggestedDiseaseCode -> 400 REQ_001
        mockMvc.perform(patch("/api/v1/admin/feedbacks/{id}/verify", pending.getId())
                        .header("Authorization", "Bearer " + expertToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isAccurate\":true,\"suggestedDiseaseCode\":\"POTATO_EARLY_BLIGHT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("REQ_001")));

        // Rejects expertNotes > 2000 chars -> 400 REQ_001
        mockMvc.perform(patch("/api/v1/admin/feedbacks/{id}/verify", pending.getId())
                        .header("Authorization", "Bearer " + expertToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expertNotes\":\"" + "x".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("REQ_001")));

        // Empty body PATCH preserves existing isAccurate=false & suggestedDisease
        mockMvc.perform(patch("/api/v1/admin/feedbacks/{id}/verify", pending.getId())
                        .header("Authorization", "Bearer " + expertToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expertVerified", is(true)))
                .andExpect(jsonPath("$.data.isAccurate", is(false)))
                .andExpect(jsonPath("$.data.suggestedDiseaseCode", is("POTATO_EARLY_BLIGHT")))
                .andExpect(jsonPath("$.data.verifiedByUsername", is("fb_expert")));

        // Re-verify marking accurate=true clears suggestedDisease
        mockMvc.perform(patch("/api/v1/admin/feedbacks/{id}/verify", pending.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isAccurate\":true,\"expertNotes\":\"Actually late blight\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isAccurate", is(true)))
                .andExpect(jsonPath("$.data.suggestedDiseaseId", nullValue()))
                .andExpect(jsonPath("$.data.suggestedDiseaseCode", nullValue()))
                .andExpect(jsonPath("$.data.verifiedByUsername", is("fb_admin")));

        List<AdminAuditEvent> events = applicationEvents.stream(AdminAuditEvent.class).toList();
        assertThat(events).hasSize(2);
        assertThat(events.get(0).action()).isEqualTo("VERIFY_FEEDBACK");
        assertThat(events.get(0).actorUsername()).isEqualTo("fb_expert");
        assertThat(events.get(1).actorUsername()).isEqualTo("fb_admin");
    }

    @Test
    @DisplayName("GET /api/v1/admin/feedbacks/export - Excludes unverified even if ?expertVerified=false, supports JSON & CSV with formula escaping and batch boundingBoxesJson")
    void exportVerifiedDataset_JsonAndCsvAndSecurity() throws Exception {
        // Empty dataset -> valid JSON [] and CSV header
        mockMvc.perform(get("/api/v1/admin/feedbacks/export")
                        .header("Authorization", "Bearer " + expertToken)
                        .param("format", "json"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("active-learning-dataset.json")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string("[]"));

        mockMvc.perform(get("/api/v1/admin/feedbacks/export")
                        .header("Authorization", "Bearer " + expertToken)
                        .param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("active-learning-dataset.csv")))
                .andExpect(content().string(containsString("feedbackId,diagnosisId,originalImageUrl")));

        // Seed 1 unverified + 1 verified feedback with formula injection payload in expertNotes
        feedbackRepository.save(DiagnosisFeedback.builder()
                .diagnosis(farmerBDiagnosis)
                .user(farmerB)
                .isAccurate(true)
                .expertVerified(false)
                .build());

        DiagnosisFeedback verified = feedbackRepository.save(DiagnosisFeedback.builder()
                .diagnosis(farmerADiagnosis)
                .user(farmerA)
                .isAccurate(false)
                .suggestedDisease(earlyBlightDisease)
                .expertVerified(true)
                .verifiedByUser(expertUser)
                .verifiedAt(Instant.now())
                .expertNotes("=HYPERLINK(\"http://evil\",\"click\")")
                .build());

        // Request export with ?expertVerified=false -> must STILL exclude unverified feedback!
        MvcResult jsonResult = mockMvc.perform(get("/api/v1/admin/feedbacks/export")
                        .header("Authorization", "Bearer " + expertToken)
                        .param("format", "json")
                        .param("expertVerified", "false"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonArray = objectMapper.readTree(jsonResult.getResponse().getContentAsString());
        assertThat(jsonArray.isArray()).isTrue();
        assertThat(jsonArray).hasSize(1);
        assertThat(jsonArray.get(0).get("feedbackId").asText()).isEqualTo(verified.getId().toString());
        assertThat(jsonArray.get(0).get("predictedDiseaseCode").asText()).isEqualTo("POTATO_LATE_BLIGHT");
        assertThat(jsonArray.get(0).get("verifiedDiseaseCode").asText()).isEqualTo("POTATO_EARLY_BLIGHT");
        JsonNode boxes = objectMapper.readTree(jsonArray.get(0).get("boundingBoxesJson").asText());
        assertThat(boxes).hasSize(1);
        assertThat(boxes.get(0).get("xMin").asDouble()).isEqualTo(240.0);
        assertThat(boxes.get(0).get("yMin").asDouble()).isEqualTo(180.0);

        // CSV export neutralizes formula injection
        mockMvc.perform(get("/api/v1/admin/feedbacks/export")
                        .header("Authorization", "Bearer " + expertToken)
                        .param("format", "csv"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("'=HYPERLINK(\"\"http://evil\"\",\"\"click\"\")")));

        // Unsupported format -> 400 REQ_001
        mockMvc.perform(get("/api/v1/admin/feedbacks/export")
                        .header("Authorization", "Bearer " + expertToken)
                        .param("format", "xml"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("REQ_001")));

        // Invalid date range on export -> 400 REQ_001
        mockMvc.perform(get("/api/v1/admin/feedbacks/export")
                        .header("Authorization", "Bearer " + expertToken)
                        .param("fromDate", "2026-12-02T00:00:00Z")
                        .param("toDate", "2026-12-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("REQ_001")));

        // Healthy diagnosis with null-disease bounding box exports cleanly without LazyInitializationException
        Diagnosis healthyDiag = createDiagnosis(farmerA, potatoPlant, null, "https://s3.eiu.edu.vn/healthy.jpg", false);
        feedbackRepository.save(DiagnosisFeedback.builder()
                .diagnosis(healthyDiag)
                .user(farmerA)
                .isAccurate(true)
                .expertVerified(true)
                .verifiedByUser(expertUser)
                .verifiedAt(Instant.now().plusSeconds(5))
                .build());

        MvcResult healthyExport = mockMvc.perform(get("/api/v1/admin/feedbacks/export")
                        .header("Authorization", "Bearer " + expertToken)
                        .param("format", "json")
                        .param("isAccurate", "true"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode healthyRows = objectMapper.readTree(healthyExport.getResponse().getContentAsString());
        assertThat(healthyRows).hasSize(1);
        assertThat(healthyRows.get(0).get("predictedDiseaseCode").asText()).isEqualTo("HEALTHY");
        assertThat(healthyRows.get(0).get("verifiedDiseaseCode").asText()).isEqualTo("HEALTHY");
        JsonNode healthyBoxes = objectMapper.readTree(healthyRows.get(0).get("boundingBoxesJson").asText());
        assertThat(healthyBoxes).hasSize(1);
        assertThat(healthyBoxes.get(0).get("diseaseCode").isNull()).isTrue();
    }
}
