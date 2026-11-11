package com.eiu.pdds.treatment.controller;

import com.eiu.pdds.common.api.ApiResponse;
import com.eiu.pdds.dto.treatment.TreatmentCreateRequest;
import com.eiu.pdds.dto.treatment.TreatmentDto;
import com.eiu.pdds.dto.treatment.TreatmentUpdateRequest;
import com.eiu.pdds.treatment.entity.TreatmentType;
import com.eiu.pdds.treatment.service.TreatmentManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing agricultural treatment protocols and disease mitigations.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Treatment Management", description = "Endpoints for managing plant pathology treatments, remedies, and dosages")
public class TreatmentController {

    private final TreatmentManagementService treatmentManagementService;

    @GetMapping("/diseases/{diseaseId}/treatments")
    @Operation(
            summary = "Get treatments for a disease",
            description = "Retrieves active treatment protocols for a specific plant disease, optionally filtered by treatment type and crop plant"
    )
    public ResponseEntity<ApiResponse<List<TreatmentDto>>> getTreatmentsByDiseaseId(
            @Parameter(description = "Disease UUID", example = "33333333-3333-3333-3333-333333333301")
            @PathVariable UUID diseaseId,

            @Parameter(description = "Optional treatment category filter (BIOLOGICAL, CHEMICAL, PREVENTIVE, CULTURAL)", example = "CHEMICAL")
            @RequestParam(required = false) TreatmentType treatmentType,

            @Parameter(description = "Optional plant crop UUID filter", example = "11111111-1111-1111-1111-111111111101")
            @RequestParam(required = false) UUID plantId
    ) {
        List<TreatmentDto> treatments = treatmentManagementService.getTreatmentsByDiseaseId(diseaseId, treatmentType, plantId);
        return ResponseEntity.ok(ApiResponse.success("Treatments retrieved successfully", treatments));
    }

    @PostMapping("/diseases/{diseaseId}/treatments")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Create treatment protocol",
            description = "Creates a new agricultural treatment protocol for a plant disease. Requires ROLE_ADMIN or ROLE_EXPERT."
    )
    public ResponseEntity<ApiResponse<TreatmentDto>> createTreatment(
            @Parameter(description = "Disease UUID", example = "33333333-3333-3333-3333-333333333301")
            @PathVariable UUID diseaseId,

            @Valid @RequestBody TreatmentCreateRequest request
    ) {
        TreatmentDto created = treatmentManagementService.createTreatment(diseaseId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Treatment created successfully", created));
    }

    @PutMapping("/treatments/{treatmentId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Update treatment protocol",
            description = "Updates an existing agricultural treatment protocol. Requires ROLE_ADMIN or ROLE_EXPERT."
    )
    public ResponseEntity<ApiResponse<TreatmentDto>> updateTreatment(
            @Parameter(description = "Treatment UUID", example = "44444444-4444-4444-4444-444444444401")
            @PathVariable UUID treatmentId,

            @Valid @RequestBody TreatmentUpdateRequest request
    ) {
        TreatmentDto updated = treatmentManagementService.updateTreatment(treatmentId, request);
        return ResponseEntity.ok(ApiResponse.success("Treatment updated successfully", updated));
    }

    @DeleteMapping("/treatments/{treatmentId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Delete treatment protocol",
            description = "Soft-deletes an agricultural treatment protocol while preserving audit trails. Requires ROLE_ADMIN or ROLE_EXPERT."
    )
    public ResponseEntity<ApiResponse<Void>> deleteTreatment(
            @Parameter(description = "Treatment UUID", example = "44444444-4444-4444-4444-444444444401")
            @PathVariable UUID treatmentId
    ) {
        treatmentManagementService.deleteTreatment(treatmentId);
        return ResponseEntity.ok(ApiResponse.success("Treatment deleted successfully", null));
    }
}
