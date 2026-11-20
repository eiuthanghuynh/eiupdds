package com.eiu.pdds.feedback.controller;

import com.eiu.pdds.auth.security.UserPrincipal;
import com.eiu.pdds.common.api.ApiResponse;
import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.feedback.dto.FeedbackResponseDto;
import com.eiu.pdds.feedback.dto.FeedbackSubmitRequest;
import com.eiu.pdds.feedback.dto.FeedbackSubmitResult;
import com.eiu.pdds.feedback.service.UserFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/diagnoses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Feedback & Active Learning", description = "Endpoints for submitting and retrieving farmer feedback on diagnosis results")
public class FeedbackController {

    private final UserFeedbackService userFeedbackService;

    @PostMapping("/{diagnosisId}/feedback")
    @Operation(summary = "Submit or update diagnosis feedback", description = "Allows the diagnosis owner to submit or update feedback until an expert verifies it.")
    public ResponseEntity<ApiResponse<FeedbackResponseDto>> submitFeedback(
            @PathVariable UUID diagnosisId,
            @Valid @RequestBody FeedbackSubmitRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        FeedbackSubmitResult result = userFeedbackService.submitOrUpdateFeedback(diagnosisId, principal.id(), request);
        if (result.created()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Feedback submitted successfully", result.feedback()));
        }
        return ResponseEntity.ok(ApiResponse.success("Feedback updated successfully", result.feedback()));
    }

    @GetMapping("/{diagnosisId}/feedback")
    @Operation(summary = "Get my diagnosis feedback", description = "Retrieves the authenticated owner's feedback for a diagnosis session.")
    public ResponseEntity<ApiResponse<FeedbackResponseDto>> getMyFeedback(
            @PathVariable UUID diagnosisId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        FeedbackResponseDto feedback = userFeedbackService.getMyFeedback(diagnosisId, principal.id());
        return ResponseEntity.ok(ApiResponse.success("Feedback retrieved successfully", feedback));
    }
}
