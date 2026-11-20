package com.eiu.pdds.feedback.service;

import com.eiu.pdds.common.api.PageResponse;
import com.eiu.pdds.feedback.dto.ExportPayload;
import com.eiu.pdds.feedback.dto.FeedbackQueueFilter;
import com.eiu.pdds.feedback.dto.FeedbackResponseDto;
import com.eiu.pdds.feedback.dto.FeedbackVerifyRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminFeedbackService {

    PageResponse<FeedbackResponseDto> getFeedbackQueue(FeedbackQueueFilter filter, Pageable pageable);

    FeedbackResponseDto verifyFeedback(UUID feedbackId, UUID verifierUserId, String verifierUsername, FeedbackVerifyRequest request);

    ExportPayload exportVerifiedDataset(String format, FeedbackQueueFilter filter);
}
