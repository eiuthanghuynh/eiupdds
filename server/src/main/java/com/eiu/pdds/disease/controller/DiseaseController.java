package com.eiu.pdds.disease.controller;

import com.eiu.pdds.common.api.ApiResponse;
import com.eiu.pdds.common.api.PageResponse;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.disease.service.DiseaseManagementService;
import com.eiu.pdds.disease.service.DiseaseQueryService;
import com.eiu.pdds.dto.disease.DiseaseCreateRequest;
import com.eiu.pdds.dto.disease.DiseaseDetailDto;
import com.eiu.pdds.dto.disease.DiseaseFilter;
import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import com.eiu.pdds.dto.disease.DiseaseUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST Controller for querying and managing plant disease catalog entries.
 * Enforces CQRS-lite separation: read queries routed to DiseaseQueryService,
 * write/management operations routed to DiseaseManagementService.
 */
@RestController
@RequestMapping("/api/v1/diseases")
@RequiredArgsConstructor
@Tag(name = "Disease Catalog", description = "Endpoints for plant disease catalog, morphological symptoms, and agricultural treatments")
public class DiseaseController {

    private final DiseaseQueryService diseaseQueryService;
    private final DiseaseManagementService diseaseManagementService;

    @GetMapping
    @Operation(
            summary = "Get paginated diseases",
            description = "Retrieves the catalog of plant diseases recognized by EIU PDDS with optional dynamic filtering and pagination"
    )
    public ResponseEntity<ApiResponse<PageResponse<DiseaseSummaryDto>>> getDiseases(
            @Parameter(description = "Keyword search matching disease code, common name, or scientific name", example = "blight")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "Associated crop code to filter diseases", example = "POTATO")
            @RequestParam(required = false) String plantCode,

            @Parameter(description = "Pathological severity level filter", example = "CRITICAL")
            @RequestParam(required = false) Severity severity,

            @Parameter(description = "Zero-based page index", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "The size of the page to be returned", example = "20")
            @RequestParam(defaultValue = "20") int size,

            @Parameter(description = "Whether to return unpaged results", example = "false")
            @RequestParam(defaultValue = "false") boolean unpaged
    ) {
        DiseaseFilter filter = new DiseaseFilter(keyword, plantCode, severity);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "commonName"));
        PageResponse<DiseaseSummaryDto> response = diseaseQueryService.getDiseases(filter, pageable, unpaged);
        return ResponseEntity.ok(ApiResponse.success("Diseases retrieved successfully", response));
    }

    @GetMapping("/{idOrCode}")
    @Operation(
            summary = "Get disease details by ID or code",
            description = "Retrieves pathology details, symptoms, and actionable biological/chemical treatment protocols by UUID or disease code"
    )
    public ResponseEntity<ApiResponse<DiseaseDetailDto>> getDiseaseByIdOrCode(
            @Parameter(description = "Standardized disease code or UUID (e.g., POTATO_LATE_BLIGHT)", example = "POTATO_LATE_BLIGHT")
            @PathVariable String idOrCode
    ) {
        DiseaseDetailDto disease = diseaseQueryService.getDiseaseByIdOrCode(idOrCode);
        return ResponseEntity.ok(ApiResponse.success("Disease details retrieved successfully", disease));
    }

    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create disease catalog record",
            description = "Registers a new plant disease catalog entry with optional host plant associations. Requires ROLE_ADMIN or ROLE_EXPERT."
    )
    public ResponseEntity<ApiResponse<DiseaseDetailDto>> createDisease(
            @Valid @RequestBody DiseaseCreateRequest request
    ) {
        DiseaseDetailDto created = diseaseManagementService.createDisease(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Disease created successfully", created));
    }

    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Update disease catalog record",
            description = "Updates an existing plant disease catalog entry and plant associations. Requires ROLE_ADMIN or ROLE_EXPERT."
    )
    public ResponseEntity<ApiResponse<DiseaseDetailDto>> updateDisease(
            @Parameter(description = "Disease UUID", example = "33333333-3333-3333-3333-333333333301")
            @PathVariable UUID id,
            @Valid @RequestBody DiseaseUpdateRequest request
    ) {
        DiseaseDetailDto updated = diseaseManagementService.updateDisease(id, request);
        return ResponseEntity.ok(ApiResponse.success("Disease updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Delete disease catalog record",
            description = "Soft-deletes a plant disease catalog entry while preserving historical records and AI telemetry. Requires ROLE_ADMIN or ROLE_EXPERT."
    )
    public ResponseEntity<ApiResponse<Void>> deleteDisease(
            @Parameter(description = "Disease UUID", example = "33333333-3333-3333-3333-333333333301")
            @PathVariable UUID id
    ) {
        diseaseManagementService.deleteDisease(id);
        return ResponseEntity.ok(ApiResponse.success("Disease deleted successfully", null));
    }
}
