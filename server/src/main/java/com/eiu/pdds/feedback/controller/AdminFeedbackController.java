package com.eiu.pdds.feedback.controller;

import com.eiu.pdds.auth.security.UserPrincipal;
import com.eiu.pdds.common.api.ApiResponse;
import com.eiu.pdds.common.api.PageResponse;
import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.feedback.dto.ExportPayload;
import com.eiu.pdds.feedback.dto.FeedbackQueueFilter;
import com.eiu.pdds.feedback.dto.FeedbackResponseDto;
import com.eiu.pdds.feedback.dto.FeedbackVerifyRequest;
import com.eiu.pdds.feedback.service.AdminFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/feedbacks")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_EXPERT')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Feedback Moderation & Active Learning", description = "Admin and expert moderation queue, verification, and active-learning dataset export")
public class AdminFeedbackController {

    private final AdminFeedbackService adminFeedbackService;

    @GetMapping
    @Operation(summary = "List feedback moderation queue", description = "Retrieves paginated diagnosis feedbacks filtered by verification state, accuracy, crop code, and date range.")
    public ResponseEntity<ApiResponse<PageResponse<FeedbackResponseDto>>> getFeedbackQueue(
            @ModelAttribute FeedbackQueueFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<FeedbackResponseDto> queue = adminFeedbackService.getFeedbackQueue(filter, pageable);
        return ResponseEntity.ok(ApiResponse.success("Feedback queue retrieved successfully", queue));
    }

    @PatchMapping("/{id}/verify")
    @Operation(summary = "Verify diagnosis feedback", description = "Marks a feedback record as expert-verified, optionally overriding accuracy, disease label, and expert notes.")
    public ResponseEntity<ApiResponse<FeedbackResponseDto>> verifyFeedback(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) FeedbackVerifyRequest request,
            @AuthenticationPrincipal UserPrincipal currentVerifier
    ) {
        if (currentVerifier == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        FeedbackVerifyRequest safeRequest = request != null
                ? request
                : new FeedbackVerifyRequest(null, null, null, null);
        FeedbackResponseDto verified = adminFeedbackService.verifyFeedback(
                id,
                currentVerifier.id(),
                currentVerifier.username(),
                safeRequest
        );
        return ResponseEntity.ok(ApiResponse.success("Feedback verified successfully", verified));
    }

    @GetMapping("/export")
    @Operation(summary = "Export verified active-learning dataset", description = "Exports expert-verified feedbacks with batch-loaded bounding boxes as JSON or RFC-4180 CSV.")
    public ResponseEntity<byte[]> exportVerifiedDataset(
            @RequestParam(defaultValue = "json") String format,
            @ModelAttribute FeedbackQueueFilter filter
    ) {
        ExportPayload payload = adminFeedbackService.exportVerifiedDataset(format, filter);
        return ResponseEntity.ok()
                .contentType(payload.mediaType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + payload.filename() + "\"")
                .body(payload.content());
    }
}
