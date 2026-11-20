package com.eiu.pdds.feedback.service.impl;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.feedback.dto.FeedbackResponseDto;
import com.eiu.pdds.feedback.dto.FeedbackSubmitRequest;
import com.eiu.pdds.feedback.dto.FeedbackSubmitResult;
import com.eiu.pdds.feedback.entity.DiagnosisFeedback;
import com.eiu.pdds.feedback.repository.DiagnosisFeedbackRepository;
import com.eiu.pdds.feedback.service.FeedbackDiseaseResolver;
import com.eiu.pdds.feedback.service.FeedbackMapper;
import com.eiu.pdds.feedback.service.UserFeedbackService;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserFeedbackServiceImpl implements UserFeedbackService {

    private final DiagnosisRepository diagnosisRepository;
    private final DiagnosisFeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final FeedbackDiseaseResolver diseaseResolver;
    private final FeedbackMapper feedbackMapper;

    @Override
    @Transactional
    public FeedbackSubmitResult submitOrUpdateFeedback(UUID diagnosisId, UUID currentUserId, FeedbackSubmitRequest request) {
        if (diagnosisId == null || currentUserId == null || request == null || request.isAccurate() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PAYLOAD);
        }

        Diagnosis diagnosis = diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull(diagnosisId, currentUserId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.DIAGNOSIS_NOT_FOUND,
                        "Diagnosis session not found or access denied"
                ));

        User submitter = userRepository.findById(currentUserId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Optional<DiagnosisFeedback> existingOpt =
                feedbackRepository.findByDiagnosisIdAndUserIdAndDeletedAtIsNull(diagnosisId, currentUserId);

        if (existingOpt.isPresent() && Boolean.TRUE.equals(existingOpt.get().getExpertVerified())) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "Verified feedback cannot be modified");
        }

        Disease suggestedDisease = diseaseResolver.resolve(
                request.isAccurate(),
                request.suggestedDiseaseId(),
                request.suggestedDiseaseCode()
        );
        String comment = normalizeOptionalText(request.userComment());

        boolean created = existingOpt.isEmpty();
        DiagnosisFeedback entity = existingOpt.orElseGet(() -> DiagnosisFeedback.builder()
                .diagnosis(diagnosis)
                .user(submitter)
                .expertVerified(false)
                .build());

        entity.setIsAccurate(request.isAccurate());
        entity.setSuggestedDisease(suggestedDisease);
        entity.setUserComment(comment);

        DiagnosisFeedback saved = feedbackRepository.saveAndFlush(entity);
        return new FeedbackSubmitResult(feedbackMapper.toDto(saved), created);
    }

    @Override
    @Transactional(readOnly = true)
    public FeedbackResponseDto getMyFeedback(UUID diagnosisId, UUID currentUserId) {
        if (diagnosisId == null || currentUserId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PAYLOAD);
        }

        diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull(diagnosisId, currentUserId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.DIAGNOSIS_NOT_FOUND,
                        "Diagnosis session not found or access denied"
                ));

        DiagnosisFeedback feedback = feedbackRepository
                .findByDiagnosisIdAndUserIdAndDeletedAtIsNull(diagnosisId, currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEEDBACK_NOT_FOUND));

        return feedbackMapper.toDto(feedback);
    }

    private String normalizeOptionalText(String raw) {
        return StringUtils.hasText(raw) ? raw.trim() : null;
    }
}
