package com.eiu.pdds.feedback.service;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.feedback.dto.FeedbackResponseDto;
import com.eiu.pdds.feedback.dto.FeedbackSubmitRequest;
import com.eiu.pdds.feedback.dto.FeedbackSubmitResult;
import com.eiu.pdds.feedback.entity.DiagnosisFeedback;
import com.eiu.pdds.feedback.repository.DiagnosisFeedbackRepository;
import com.eiu.pdds.feedback.service.impl.UserFeedbackServiceImpl;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserFeedbackServiceTest {

    @Mock
    private DiagnosisRepository diagnosisRepository;

    @Mock
    private DiagnosisFeedbackRepository feedbackRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FeedbackDiseaseResolver diseaseResolver;

    private UserFeedbackServiceImpl service;

    private UUID userId;
    private UUID diagnosisId;
    private User owner;
    private Plant plant;
    private Disease lateBlight;
    private Disease earlyBlight;
    private Diagnosis diagnosis;

    @BeforeEach
    void setUp() {
        service = new UserFeedbackServiceImpl(
                diagnosisRepository,
                feedbackRepository,
                userRepository,
                diseaseResolver,
                new FeedbackMapper()
        );

        userId = UUID.randomUUID();
        diagnosisId = UUID.randomUUID();

        owner = User.builder()
                .username("farmer_tuan")
                .email("tuan@farm.vn")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        owner.setId(userId);

        plant = Plant.builder()
                .plantCode("POTATO")
                .commonName("Potato")
                .build();
        plant.setId(UUID.randomUUID());

        lateBlight = Disease.builder()
                .diseaseCode("POTATO_LATE_BLIGHT")
                .commonName("Potato late blight")
                .severity(Severity.CRITICAL)
                .build();
        lateBlight.setId(UUID.randomUUID());

        earlyBlight = Disease.builder()
                .diseaseCode("POTATO_EARLY_BLIGHT")
                .commonName("Potato early blight")
                .severity(Severity.HIGH)
                .build();
        earlyBlight.setId(UUID.randomUUID());

        diagnosis = Diagnosis.builder()
                .user(owner)
                .plant(plant)
                .originalImageUrl("https://cdn.eiu.edu.vn/leaf.jpg")
                .dominantDisease(lateBlight)
                .dominantConfidence(0.91)
                .build();
        diagnosis.setId(diagnosisId);
    }

    @Test
    @DisplayName("Creates new feedback when user owns diagnosis and no feedback exists yet")
    void submitOrUpdateFeedback_WhenNew_CreatesFeedback() {
        FeedbackSubmitRequest request = new FeedbackSubmitRequest(false, null, "POTATO_EARLY_BLIGHT", "  Concentric rings visible  ");

        when(diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull(diagnosisId, userId))
                .thenReturn(Optional.of(diagnosis));
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(feedbackRepository.findByDiagnosisIdAndUserIdAndDeletedAtIsNull(diagnosisId, userId))
                .thenReturn(Optional.empty());
        when(diseaseResolver.resolve(false, null, "POTATO_EARLY_BLIGHT")).thenReturn(earlyBlight);
        when(feedbackRepository.saveAndFlush(any(DiagnosisFeedback.class))).thenAnswer(inv -> {
            DiagnosisFeedback saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            saved.setCreatedAt(Instant.now());
            saved.setUpdatedAt(Instant.now());
            return saved;
        });

        FeedbackSubmitResult result = service.submitOrUpdateFeedback(diagnosisId, userId, request);

        assertThat(result.created()).isTrue();
        assertThat(result.feedback().isAccurate()).isFalse();
        assertThat(result.feedback().suggestedDiseaseCode()).isEqualTo("POTATO_EARLY_BLIGHT");
        assertThat(result.feedback().userComment()).isEqualTo("Concentric rings visible");
        assertThat(result.feedback().expertVerified()).isFalse();
        assertThat(result.feedback().submittedByUsername()).isEqualTo("farmer_tuan");
        assertThat(result.feedback().plantCode()).isEqualTo("POTATO");
        assertThat(result.feedback().predictedDiseaseCode()).isEqualTo("POTATO_LATE_BLIGHT");
    }

    @Test
    @DisplayName("Updates existing unverified feedback and normalizes blank comment to null")
    void submitOrUpdateFeedback_WhenUnverifiedExists_UpdatesFeedback() {
        DiagnosisFeedback existing = DiagnosisFeedback.builder()
                .diagnosis(diagnosis)
                .user(owner)
                .isAccurate(false)
                .suggestedDisease(earlyBlight)
                .userComment("Old comment")
                .expertVerified(false)
                .build();
        existing.setId(UUID.randomUUID());

        FeedbackSubmitRequest request = new FeedbackSubmitRequest(true, null, null, "   ");

        when(diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull(diagnosisId, userId))
                .thenReturn(Optional.of(diagnosis));
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(feedbackRepository.findByDiagnosisIdAndUserIdAndDeletedAtIsNull(diagnosisId, userId))
                .thenReturn(Optional.of(existing));
        when(diseaseResolver.resolve(true, null, null)).thenReturn(null);
        when(feedbackRepository.saveAndFlush(any(DiagnosisFeedback.class))).thenAnswer(inv -> inv.getArgument(0));

        FeedbackSubmitResult result = service.submitOrUpdateFeedback(diagnosisId, userId, request);

        assertThat(result.created()).isFalse();
        assertThat(result.feedback().isAccurate()).isTrue();
        assertThat(result.feedback().suggestedDiseaseId()).isNull();
        assertThat(result.feedback().userComment()).isNull();

        ArgumentCaptor<DiagnosisFeedback> captor = ArgumentCaptor.forClass(DiagnosisFeedback.class);
        verify(feedbackRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(existing.getId());
    }

    @Test
    @DisplayName("Throws 409 RES_002 when user tries to modify expert-verified feedback")
    void submitOrUpdateFeedback_WhenAlreadyVerified_ThrowsConflict() {
        DiagnosisFeedback verified = DiagnosisFeedback.builder()
                .diagnosis(diagnosis)
                .user(owner)
                .isAccurate(true)
                .expertVerified(true)
                .build();
        verified.setId(UUID.randomUUID());

        FeedbackSubmitRequest request = new FeedbackSubmitRequest(false, null, "POTATO_EARLY_BLIGHT", "Changed mind");

        when(diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull(diagnosisId, userId))
                .thenReturn(Optional.of(diagnosis));
        when(userRepository.findById(userId)).thenReturn(Optional.of(owner));
        when(feedbackRepository.findByDiagnosisIdAndUserIdAndDeletedAtIsNull(diagnosisId, userId))
                .thenReturn(Optional.of(verified));

        assertThatThrownBy(() -> service.submitOrUpdateFeedback(diagnosisId, userId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.DUPLICATE_RESOURCE));

        verifyNoInteractions(diseaseResolver);
    }

    @Test
    @DisplayName("Throws 404 DIAGNOSIS_001 on IDOR, guest, or soft-deleted diagnosis")
    void submitOrUpdateFeedback_WhenDiagnosisNotOwned_ThrowsDiagnosisNotFound() {
        when(diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull(diagnosisId, userId))
                .thenReturn(Optional.empty());

        FeedbackSubmitRequest request = new FeedbackSubmitRequest(true, null, null, null);

        assertThatThrownBy(() -> service.submitOrUpdateFeedback(diagnosisId, userId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.DIAGNOSIS_NOT_FOUND));

        verifyNoInteractions(feedbackRepository, diseaseResolver);
    }

    @Test
    @DisplayName("getMyFeedback returns DTO for owner when feedback exists")
    void getMyFeedback_Success() {
        DiagnosisFeedback existing = DiagnosisFeedback.builder()
                .diagnosis(diagnosis)
                .user(owner)
                .isAccurate(true)
                .expertVerified(false)
                .userComment("Accurate result")
                .build();
        existing.setId(UUID.randomUUID());

        when(diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull(diagnosisId, userId))
                .thenReturn(Optional.of(diagnosis));
        when(feedbackRepository.findByDiagnosisIdAndUserIdAndDeletedAtIsNull(diagnosisId, userId))
                .thenReturn(Optional.of(existing));

        FeedbackResponseDto dto = service.getMyFeedback(diagnosisId, userId);

        assertThat(dto.id()).isEqualTo(existing.getId());
        assertThat(dto.diagnosisId()).isEqualTo(diagnosisId);
        assertThat(dto.userComment()).isEqualTo("Accurate result");
    }

    @Test
    @DisplayName("getMyFeedback throws 404 FEEDBACK_001 when owner has not submitted feedback")
    void getMyFeedback_WhenMissing_ThrowsFeedbackNotFound() {
        when(diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull(diagnosisId, userId))
                .thenReturn(Optional.of(diagnosis));
        when(feedbackRepository.findByDiagnosisIdAndUserIdAndDeletedAtIsNull(diagnosisId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyFeedback(diagnosisId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.FEEDBACK_NOT_FOUND));
    }
}
