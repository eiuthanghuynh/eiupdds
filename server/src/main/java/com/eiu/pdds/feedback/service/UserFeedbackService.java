package com.eiu.pdds.feedback.service;

import com.eiu.pdds.feedback.dto.FeedbackResponseDto;
import com.eiu.pdds.feedback.dto.FeedbackSubmitRequest;
import com.eiu.pdds.feedback.dto.FeedbackSubmitResult;

import java.util.UUID;

public interface UserFeedbackService {

    FeedbackSubmitResult submitOrUpdateFeedback(UUID diagnosisId, UUID currentUserId, FeedbackSubmitRequest request);

    FeedbackResponseDto getMyFeedback(UUID diagnosisId, UUID currentUserId);
}
