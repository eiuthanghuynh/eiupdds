package com.eiu.pdds.disease.controller;

import com.eiu.pdds.common.api.ApiResponse;
import com.eiu.pdds.disease.service.DiseaseService;
import com.eiu.pdds.dto.disease.DiseaseDetailDto;
import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/diseases")
@RequiredArgsConstructor
@Tag(name = "Disease Catalog", description = "Endpoints for plant disease catalog, morphological symptoms, and agricultural treatments")
public class DiseaseController {

    private final DiseaseService diseaseService;

    @GetMapping
    @Operation(summary = "Get all diseases", description = "Retrieves the standardized catalog of plant diseases recognized by EIU PDDS")
    public ResponseEntity<ApiResponse<List<DiseaseSummaryDto>>> getAllDiseases() {
        List<DiseaseSummaryDto> diseases = diseaseService.getAllDiseases();
        return ResponseEntity.ok(ApiResponse.success("Diseases retrieved successfully", diseases));
    }

    @GetMapping("/{diseaseCode}")
    @Operation(summary = "Get disease details by code", description = "Retrieves pathology details, symptoms, and actionable biological/chemical treatment protocols")
    public ResponseEntity<ApiResponse<DiseaseDetailDto>> getDiseaseByCode(
            @Parameter(description = "Standardized disease code (e.g., POTATO_LATE_BLIGHT)", example = "POTATO_LATE_BLIGHT")
            @PathVariable String diseaseCode) {
        DiseaseDetailDto disease = diseaseService.getDiseaseByCode(diseaseCode);
        return ResponseEntity.ok(ApiResponse.success("Disease details retrieved successfully", disease));
    }
}
