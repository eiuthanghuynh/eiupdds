package com.eiu.pdds.feedback.service;

import com.eiu.pdds.audit.event.AdminAuditEvent;
import com.eiu.pdds.common.api.PageResponse;
import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.diagnosis.entity.BoundingBox;
import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.repository.BoundingBoxRepository;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.feedback.dto.ActiveLearningExportFormat;
import com.eiu.pdds.feedback.dto.ActiveLearningExportRow;
import com.eiu.pdds.feedback.dto.ExportPayload;
import com.eiu.pdds.feedback.dto.FeedbackQueueFilter;
import com.eiu.pdds.feedback.dto.FeedbackResponseDto;
import com.eiu.pdds.feedback.dto.FeedbackVerifyRequest;
import com.eiu.pdds.feedback.entity.DiagnosisFeedback;
import com.eiu.pdds.feedback.repository.DiagnosisFeedbackRepository;
import com.eiu.pdds.feedback.service.impl.AdminFeedbackServiceImpl;
import com.eiu.pdds.feedback.service.impl.CsvActiveLearningExporter;
import com.eiu.pdds.feedback.service.impl.JsonActiveLearningExporter;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminFeedbackServiceTest {

    @Mock
    private DiagnosisFeedbackRepository feedbackRepository;

    @Mock
    private BoundingBoxRepository boundingBoxRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FeedbackDiseaseResolver diseaseResolver;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ObjectMapper objectMapper;
    private AdminFeedbackServiceImpl service;

    private User farmer;
    private User expert;
    private Plant potato;
    private Disease lateBlight;
    private Disease earlyBlight;
    private Diagnosis diagnosis;
    private DiagnosisFeedback feedback;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        List<ActiveLearningExporter> exporters = List.of(
                new JsonActiveLearningExporter(objectMapper),
                new CsvActiveLearningExporter()
        );

        service = new AdminFeedbackServiceImpl(
                feedbackRepository,
                userRepository,
                diseaseResolver,
                new FeedbackMapper(),
                new ActiveLearningDatasetAssembler(boundingBoxRepository, objectMapper),
                exporters,
                eventPublisher
        );

        farmer = User.builder().username("farmer1").email("farmer1@farm.vn").role(Role.ROLE_USER).isActive(true).build();
        farmer.setId(UUID.randomUUID());

        expert = User.builder().username("expert_lan").email("lan@eiu.edu.vn").role(Role.ROLE_EXPERT).isActive(true).build();
        expert.setId(UUID.randomUUID());

        potato = Plant.builder().plantCode("POTATO").commonName("Potato").build();
        potato.setId(UUID.randomUUID());

        lateBlight = Disease.builder().diseaseCode("POTATO_LATE_BLIGHT").commonName("Potato late blight").severity(Severity.CRITICAL).build();
        lateBlight.setId(UUID.randomUUID());

        earlyBlight = Disease.builder().diseaseCode("POTATO_EARLY_BLIGHT").commonName("Potato early blight").severity(Severity.HIGH).build();
        earlyBlight.setId(UUID.randomUUID());

        diagnosis = Diagnosis.builder()
                .user(farmer)
                .plant(potato)
                .originalImageUrl("https://s3.eiu.edu.vn/eiu-pdds-plants/potato.jpg")
                .dominantDisease(lateBlight)
                .dominantConfidence(0.84)
                .build();
        diagnosis.setId(UUID.randomUUID());

        feedback = DiagnosisFeedback.builder()
                .diagnosis(diagnosis)
                .user(farmer)
                .isAccurate(false)
                .suggestedDisease(earlyBlight)
                .userComment("Spots have concentric rings")
                .expertVerified(false)
                .build();
        feedback.setId(UUID.randomUUID());
        feedback.setCreatedAt(Instant.parse("2026-11-20T08:00:00Z"));
        feedback.setUpdatedAt(Instant.parse("2026-11-20T08:00:00Z"));
    }

    @Test
    @DisplayName("getFeedbackQueue rejects fromDate after toDate with 400 REQ_001")
    void getFeedbackQueue_WhenFromDateAfterToDate_ThrowsInvalidRequest() {
        FeedbackQueueFilter invalidFilter = new FeedbackQueueFilter(
                null,
                null,
                null,
                Instant.parse("2026-11-21T00:00:00Z"),
                Instant.parse("2026-11-20T00:00:00Z")
        );

        assertThatThrownBy(() -> service.getFeedbackQueue(invalidFilter, PageRequest.of(0, 20)))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST_PAYLOAD));
    }

    @Test
    @DisplayName("getFeedbackQueue returns mapped PageResponse")
    @SuppressWarnings("unchecked")
    void getFeedbackQueue_Success() {
        Pageable pageable = PageRequest.of(0, 20);
        when(feedbackRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(feedback), pageable, 1));

        PageResponse<FeedbackResponseDto> page = service.getFeedbackQueue(
                new FeedbackQueueFilter(false, false, "POTATO", null, null),
                pageable
        );

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).suggestedDiseaseCode()).isEqualTo("POTATO_EARLY_BLIGHT");
    }

    @Test
    @DisplayName("verifyFeedback confirms user suggestion when request omits disease and publishes AdminAuditEvent")
    void verifyFeedback_PreservesUserSuggestionAndPublishesAuditEvent() {
        when(feedbackRepository.findByIdAndDeletedAtIsNull(feedback.getId()))
                .thenReturn(Optional.of(feedback));
        when(userRepository.findById(expert.getId())).thenReturn(Optional.of(expert));
        when(feedbackRepository.saveAndFlush(any(DiagnosisFeedback.class))).thenAnswer(inv -> inv.getArgument(0));

        FeedbackResponseDto verified = service.verifyFeedback(
                feedback.getId(),
                expert.getId(),
                expert.getUsername(),
                new FeedbackVerifyRequest(null, null, null, "  Confirmed early blight lesion morphology  ")
        );

        assertThat(verified.expertVerified()).isTrue();
        assertThat(verified.isAccurate()).isFalse();
        assertThat(verified.suggestedDiseaseCode()).isEqualTo("POTATO_EARLY_BLIGHT");
        assertThat(verified.verifiedByUserId()).isEqualTo(expert.getId());
        assertThat(verified.verifiedByUsername()).isEqualTo("expert_lan");
        assertThat(verified.verifiedAt()).isNotNull();
        assertThat(verified.expertNotes()).isEqualTo("Confirmed early blight lesion morphology");

        ArgumentCaptor<AdminAuditEvent> eventCaptor = ArgumentCaptor.forClass(AdminAuditEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        AdminAuditEvent event = eventCaptor.getValue();
        assertThat(event.actorUserId()).isEqualTo(expert.getId());
        assertThat(event.actorUsername()).isEqualTo("expert_lan");
        assertThat(event.action()).isEqualTo("VERIFY_FEEDBACK");
        assertThat(event.targetType()).isEqualTo("DIAGNOSIS_FEEDBACK");
        assertThat(event.targetId()).isEqualTo(feedback.getId().toString());
        assertThat(event.beforeState()).isNotNull();
        assertThat(event.afterState()).isNotNull();
    }

    @Test
    @DisplayName("verifyFeedback clears prior suggested disease when expert marks diagnosis accurate")
    void verifyFeedback_WhenExpertMarksAccurate_ClearsSuggestedDisease() {
        when(feedbackRepository.findByIdAndDeletedAtIsNull(feedback.getId()))
                .thenReturn(Optional.of(feedback));
        when(userRepository.findById(expert.getId())).thenReturn(Optional.of(expert));
        when(diseaseResolver.resolve(true, null, null)).thenReturn(null);
        when(feedbackRepository.saveAndFlush(any(DiagnosisFeedback.class))).thenAnswer(inv -> inv.getArgument(0));

        FeedbackResponseDto verified = service.verifyFeedback(
                feedback.getId(),
                expert.getId(),
                expert.getUsername(),
                new FeedbackVerifyRequest(true, null, null, "Model was right")
        );

        assertThat(verified.expertVerified()).isTrue();
        assertThat(verified.isAccurate()).isTrue();
        assertThat(verified.suggestedDiseaseId()).isNull();
        assertThat(verified.suggestedDiseaseCode()).isNull();
    }

    @Test
    @DisplayName("verifyFeedback throws 404 FEEDBACK_001 when feedback does not exist")
    void verifyFeedback_WhenNotFound_ThrowsFeedbackNotFound() {
        UUID missingId = UUID.randomUUID();
        when(feedbackRepository.findByIdAndDeletedAtIsNull(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyFeedback(missingId, expert.getId(), expert.getUsername(), null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.FEEDBACK_NOT_FOUND));
    }

    @Test
    @DisplayName("exportVerifiedDataset rejects unsupported format with 400 REQ_001")
    void exportVerifiedDataset_InvalidFormat_ThrowsInvalidRequest() {
        assertThatThrownBy(() -> service.exportVerifiedDataset("parquet", null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.INVALID_REQUEST_PAYLOAD));
    }

    @Test
    @DisplayName("exportVerifiedDataset returns empty JSON array [] without querying bounding boxes when no rows match")
    @SuppressWarnings("unchecked")
    void exportVerifiedDataset_WhenEmptyJson_ReturnsEmptyArray() {
        when(feedbackRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of());

        ExportPayload payload = service.exportVerifiedDataset("json", null);

        assertThat(new String(payload.content(), StandardCharsets.UTF_8)).isEqualTo("[]");
        assertThat(payload.filename()).isEqualTo("active-learning-dataset.json");
        verify(boundingBoxRepository, never()).findByDiagnosisIdIn(anyCollection());
    }

    @Test
    @DisplayName("exportVerifiedDataset batch loads bounding boxes and computes HEALTHY/UNLABELED/verified disease codes")
    @SuppressWarnings("unchecked")
    void exportVerifiedDataset_BatchLoadsBoxesAndMapsDiseaseCodes() throws Exception {
        Diagnosis healthyDiag = Diagnosis.builder()
                .user(farmer)
                .plant(potato)
                .originalImageUrl("https://s3.eiu.edu.vn/healthy.jpg")
                .dominantDisease(null)
                .build();
        healthyDiag.setId(UUID.randomUUID());

        DiagnosisFeedback verifiedCorrected = DiagnosisFeedback.builder()
                .diagnosis(diagnosis)
                .user(farmer)
                .isAccurate(false)
                .suggestedDisease(earlyBlight)
                .expertVerified(true)
                .verifiedByUser(expert)
                .verifiedAt(Instant.parse("2026-11-20T11:00:00Z"))
                .expertNotes("Corrected label")
                .build();
        verifiedCorrected.setId(UUID.randomUUID());

        DiagnosisFeedback verifiedHealthy = DiagnosisFeedback.builder()
                .diagnosis(healthyDiag)
                .user(farmer)
                .isAccurate(true)
                .expertVerified(true)
                .verifiedByUser(expert)
                .verifiedAt(Instant.parse("2026-11-20T10:00:00Z"))
                .build();
        verifiedHealthy.setId(UUID.randomUUID());

        DiagnosisFeedback verifiedUnlabeled = DiagnosisFeedback.builder()
                .diagnosis(diagnosis)
                .user(farmer)
                .isAccurate(false)
                .suggestedDisease(null)
                .expertVerified(true)
                .verifiedByUser(expert)
                .verifiedAt(Instant.parse("2026-11-20T09:00:00Z"))
                .build();
        verifiedUnlabeled.setId(UUID.randomUUID());

        BoundingBox box = BoundingBox.builder()
                .diagnosis(diagnosis)
                .disease(lateBlight)
                .label("Potato leaf late blight")
                .confidence(0.84)
                .x(200.0)
                .y(250.0)
                .width(100.0)
                .height(80.0)
                .build();
        box.setId(UUID.randomUUID());

        when(feedbackRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(verifiedCorrected, verifiedHealthy, verifiedUnlabeled));
        when(boundingBoxRepository.findByDiagnosisIdIn(anyCollection()))
                .thenReturn(List.of(box));

        ExportPayload payload = service.exportVerifiedDataset("json", new FeedbackQueueFilter(false, null, null, null, null));

        verify(boundingBoxRepository).findByDiagnosisIdIn(anyCollection());

        ActiveLearningExportRow[] rows = objectMapper.readValue(payload.content(), ActiveLearningExportRow[].class);
        assertThat(rows).hasSize(3);

        assertThat(rows[0].predictedDiseaseCode()).isEqualTo("POTATO_LATE_BLIGHT");
        assertThat(rows[0].verifiedDiseaseCode()).isEqualTo("POTATO_EARLY_BLIGHT");
        JsonNode boxesNode = objectMapper.readTree(rows[0].boundingBoxesJson());
        assertThat(boxesNode).hasSize(1);
        assertThat(boxesNode.get(0).get("xMin").asDouble()).isEqualTo(150.0);
        assertThat(boxesNode.get(0).get("yMin").asDouble()).isEqualTo(210.0);
        assertThat(boxesNode.get(0).get("diseaseCode").asText()).isEqualTo("POTATO_LATE_BLIGHT");

        assertThat(rows[1].predictedDiseaseCode()).isEqualTo("HEALTHY");
        assertThat(rows[1].verifiedDiseaseCode()).isEqualTo("HEALTHY");
        assertThat(rows[1].boundingBoxesJson()).isEqualTo("[]");

        assertThat(rows[2].predictedDiseaseCode()).isEqualTo("POTATO_LATE_BLIGHT");
        assertThat(rows[2].verifiedDiseaseCode()).isEqualTo("UNLABELED");
    }

    @Test
    @DisplayName("ActiveLearningExportFormat.fromString defaults null/blank to JSON and trims case-insensitively")
    void exportFormatFromString_ParsesAndDefaults() {
        assertThat(ActiveLearningExportFormat.fromString(null)).isEqualTo(ActiveLearningExportFormat.JSON);
        assertThat(ActiveLearningExportFormat.fromString("   ")).isEqualTo(ActiveLearningExportFormat.JSON);
        assertThat(ActiveLearningExportFormat.fromString(" CsV ")).isEqualTo(ActiveLearningExportFormat.CSV);
    }
}
