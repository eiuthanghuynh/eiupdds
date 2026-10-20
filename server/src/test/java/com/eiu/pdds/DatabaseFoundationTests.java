package com.eiu.pdds;

import com.eiu.pdds.diagnosis.entity.BoundingBox;
import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.entity.DiagnosisStatus;
import com.eiu.pdds.diagnosis.repository.BoundingBoxRepository;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.PlantDisease;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.disease.repository.PlantDiseaseRepository;
import com.eiu.pdds.feedback.entity.DiagnosisFeedback;
import com.eiu.pdds.feedback.repository.DiagnosisFeedbackRepository;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.plant.repository.PlantRepository;
import com.eiu.pdds.treatment.entity.Treatment;
import com.eiu.pdds.treatment.entity.TreatmentType;
import com.eiu.pdds.treatment.repository.TreatmentRepository;
import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        TransactionalTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class
})
class DatabaseFoundationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlantRepository plantRepository;

    @Autowired
    private DiseaseRepository diseaseRepository;

    @Autowired
    private PlantDiseaseRepository plantDiseaseRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private BoundingBoxRepository boundingBoxRepository;

    @Autowired
    private DiagnosisFeedbackRepository feedbackRepository;

    @Test
    @DisplayName("Verify User Persistence, Auditing, and Soft Delete Tombstone")
    void testUserPersistenceAndAudit() {
        User user = User.builder()
                .username("thang_backend")
                .email("thang@eiu.edu.vn")
                .passwordHash("$2a$12$eiuTestHash123456789012345678901234567890")
                .fullName("Thang Backend Lead")
                .phoneNumber("+84901234567")
                .role(Role.ROLE_ADMIN)
                .isActive(true)
                .build();

        User savedUser = userRepository.saveAndFlush(user);

        assertNotNull(savedUser.getId(), "UUID primary key must be auto-generated");
        assertNotNull(savedUser.getCreatedAt(), "createdAt must be populated by auditing");
        assertNotNull(savedUser.getUpdatedAt(), "updatedAt must be populated by auditing");
        assertEquals(0L, savedUser.getVersion(), "Initial version must be 0");
        assertFalse(savedUser.isDeleted(), "Entity should not be deleted initially");

        // Test soft deletion
        savedUser.markDeleted();
        User softDeletedUser = userRepository.saveAndFlush(savedUser);
        assertTrue(softDeletedUser.isDeleted(), "Entity should be marked as deleted");
        assertTrue(userRepository.findByDeletedAtIsNull().isEmpty(), "Deleted user excluded from active query");
    }

    @Test
    @DisplayName("Verify Plant, Disease, and PlantDisease Junction Mapping")
    void testPlantAndDiseaseRelationship() {
        Plant corn = plantRepository.saveAndFlush(Plant.builder()
                .plantCode("TEST_CORN")
                .commonName("Corn")
                .scientificName("Zea mays")
                .description("Cereal grain crop")
                .build());

        Disease blight = diseaseRepository.saveAndFlush(Disease.builder()
                .diseaseCode("TEST_CORN_BLIGHT")
                .commonName("Corn northern leaf blight")
                .scientificName("Exserohilum turcicum")
                .severity(Severity.HIGH)
                .symptoms("Cigar-shaped lesions on leaves")
                .description("Severe fungal foliar blight")
                .build());

        PlantDisease junction = plantDiseaseRepository.saveAndFlush(PlantDisease.builder()
                .plant(corn)
                .disease(blight)
                .specificSymptoms("Elliptical grayish lesions on foliage")
                .riskSeason("Moderate temperature with extended dew periods")
                .build());

        assertNotNull(junction.getId(), "Junction ID must be generated");
        Optional<PlantDisease> found = plantDiseaseRepository.findByPlantIdAndDiseaseId(corn.getId(), blight.getId());
        assertTrue(found.isPresent(), "Junction record must be retrievable");
        assertEquals("Moderate temperature with extended dew periods", found.get().getRiskSeason());
    }

    @Test
    @DisplayName("Verify Treatment Linked to Disease and Plant")
    void testTreatmentPersistence() {
        Disease leafSpot = diseaseRepository.saveAndFlush(Disease.builder()
                .diseaseCode("TEST_TOMATO_LEAF_SPOT")
                .commonName("Tomato Septoria Leaf Spot")
                .severity(Severity.HIGH)
                .build());

        Treatment treatment = treatmentRepository.saveAndFlush(Treatment.builder()
                .disease(leafSpot)
                .treatmentType(TreatmentType.BIOLOGICAL)
                .title("Copper Fungicide Spray")
                .instructions("Spray every 7-10 days upon first symptom appearance")
                .activeIngredients("Copper octanoate 10%")
                .dosageRecommendation("20ml per 10L water")
                .safetyIntervalDays(3)
                .warning("Do not apply under intense direct midday sunlight")
                .build());

        assertNotNull(treatment.getId());
        List<Treatment> treatments = treatmentRepository.findByDiseaseIdAndDeletedAtIsNull(leafSpot.getId());
        assertEquals(1, treatments.size());
        assertEquals(TreatmentType.BIOLOGICAL, treatments.get(0).getTreatmentType());
    }

    @Test
    @DisplayName("Verify Diagnosis Session with BoundingBoxes, JSONB raw response, and Feedback")
    void testDiagnosisWithBoundingBoxesAndFeedback() {
        User farmer = userRepository.saveAndFlush(User.builder()
                .username("farmer_huy")
                .email("farmer_huy@eiu.edu.vn")
                .passwordHash("secret")
                .role(Role.ROLE_USER)
                .build());

        Plant tomato = plantRepository.saveAndFlush(Plant.builder()
                .plantCode("TEST_TOMATO")
                .commonName("Tomato (Cà chua)")
                .build());

        Disease lateBlight = diseaseRepository.saveAndFlush(Disease.builder()
                .diseaseCode("TEST_TOMATO_LATE_BLIGHT")
                .commonName("Late Blight")
                .severity(Severity.CRITICAL)
                .build());

        Diagnosis diagnosis = Diagnosis.builder()
                .user(farmer)
                .plant(tomato)
                .originalImageUrl("https://s3.eiu.edu.vn/plants/sample-01.jpg")
                .imageWidth(1920)
                .imageHeight(1080)
                .latitude(11.0528)
                .longitude(106.6661)
                .deviceInfo("iPhone 15 Pro / iOS 17.5")
                .dominantDisease(lateBlight)
                .dominantConfidence(0.925)
                .inferenceTimeMs(340)
                .rawAiResponse("{\"predictions\":[{\"class\":\"Tomato Late Blight\",\"confidence\":0.925,\"x\":450,\"y\":320,\"width\":180,\"height\":210}]}")
                .status(DiagnosisStatus.COMPLETED)
                .build();

        // Add bounding box
        BoundingBox box = BoundingBox.builder()
                .disease(lateBlight)
                .label("Tomato Late Blight")
                .confidence(0.925)
                .x(450.0)
                .y(320.0)
                .width(180.0)
                .height(210.0)
                .build();
        diagnosis.addBoundingBox(box);

        Diagnosis savedDiagnosis = diagnosisRepository.saveAndFlush(diagnosis);

        assertNotNull(savedDiagnosis.getId());
        assertEquals(1, savedDiagnosis.getBoundingBoxes().size());
        assertEquals(450.0, savedDiagnosis.getBoundingBoxes().get(0).getX());
        assertTrue(savedDiagnosis.getRawAiResponse().contains("Tomato Late Blight"));

        // Add Feedback
        DiagnosisFeedback feedback = DiagnosisFeedback.builder()
                .diagnosis(savedDiagnosis)
                .user(farmer)
                .isAccurate(true)
                .expertVerified(false)
                .userComment("Accurate diagnosis, matched real symptoms on field")
                .build();
        DiagnosisFeedback savedFeedback = feedbackRepository.saveAndFlush(feedback);

        assertNotNull(savedFeedback.getId());
        List<DiagnosisFeedback> feedbacks = feedbackRepository.findByDiagnosisIdAndDeletedAtIsNull(savedDiagnosis.getId());
        assertEquals(1, feedbacks.size());
        assertTrue(feedbacks.get(0).getIsAccurate());
    }
}
