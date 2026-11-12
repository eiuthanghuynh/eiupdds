package com.eiu.pdds.diagnosis.controller;

import com.eiu.pdds.auth.security.UserPrincipal;
import com.eiu.pdds.common.api.ApiResponse;
import com.eiu.pdds.common.api.PageResponse;
import com.eiu.pdds.diagnosis.service.DiagnosisQueryService;
import com.eiu.pdds.diagnosis.service.DiagnosisService;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeRequest;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import com.eiu.pdds.dto.diagnosis.DiagnosisHistoryFilter;
import com.eiu.pdds.dto.diagnosis.DiagnosisSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/diagnoses")
@RequiredArgsConstructor
@Tag(name = "Diagnosis & AI Inference", description = "Endpoints for plant disease detection, leaf image analysis, and treatment protocols")
@SecurityRequirement(name = "bearerAuth")
public class DiagnosisController {

    private final DiagnosisService diagnosisService;
    private final DiagnosisQueryService diagnosisQueryService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Analyze leaf image for plant diseases",
            description = "Receives a plant leaf image (JPEG/PNG/WebP) via multipart/form-data, performs Computer Vision inference, " +
                    "and returns normalized bounding boxes, dominant disease classification, and curated agricultural treatment protocols."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Inference completed successfully with detections and treatments",
                    content = @Content(schema = @Schema(implementation = DiagnosisAnalyzeResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid image file format or corrupt image payload"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT bearer token"
            )
    })
    public ResponseEntity<ApiResponse<DiagnosisAnalyzeResponse>> analyzeImage(
            @ModelAttribute @Valid DiagnosisAnalyzeRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal != null ? principal.id() : null;
        DiagnosisAnalyzeResponse response = diagnosisService.analyze(request.withUserId(userId));
        return ResponseEntity.ok(ApiResponse.success("Image analyzed successfully", response));
    }

    @GetMapping
    @Operation(
            summary = "Retrieve diagnosis history",
            description = "Returns paginated list of historical diagnosis sessions for the authenticated user, optionally filtered by crop, disease, status, or date range."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Diagnosis history page retrieved successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT bearer token"
            )
    })
    public ResponseEntity<ApiResponse<PageResponse<DiagnosisSummaryDto>>> getHistory(
            @ModelAttribute DiagnosisHistoryFilter filter,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal != null ? principal.id() : null;
        PageResponse<DiagnosisSummaryDto> history = diagnosisQueryService.getUserHistory(userId, filter, pageable);
        return ResponseEntity.ok(ApiResponse.success("Diagnosis history retrieved successfully", history));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Retrieve diagnosis details",
            description = "Returns complete diagnosis session data including dual bounding box coordinates and treatment protocols."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Diagnosis session details retrieved successfully",
                    content = @Content(schema = @Schema(implementation = DiagnosisAnalyzeResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT bearer token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Diagnosis session not found or does not belong to the authenticated user"
            )
    })
    public ResponseEntity<ApiResponse<DiagnosisAnalyzeResponse>> getDetail(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal != null ? principal.id() : null;
        DiagnosisAnalyzeResponse detail = diagnosisQueryService.getUserDiagnosisDetail(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Diagnosis details retrieved successfully", detail));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Soft delete diagnosis session",
            description = "Soft-deletes a diagnosis session from the user's history, preserving the record as an AI tombstone."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Diagnosis session deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Missing or invalid JWT bearer token"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Diagnosis session not found or does not belong to the authenticated user"
            )
    })
    public ResponseEntity<ApiResponse<Void>> deleteDiagnosis(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UUID userId = principal != null ? principal.id() : null;
        diagnosisService.softDeleteUserDiagnosis(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Diagnosis session deleted successfully", null));
    }
}
