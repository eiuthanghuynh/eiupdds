package com.eiu.pdds.diagnosis.controller;

import com.eiu.pdds.common.api.ApiResponse;
import com.eiu.pdds.diagnosis.service.DiagnosisService;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeRequest;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/diagnoses")
@RequiredArgsConstructor
@Tag(name = "Diagnosis & AI Inference", description = "Endpoints for plant disease detection, leaf image analysis, and treatment protocols")
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Analyze leaf image for plant diseases",
            description = "Receives a plant leaf image (JPEG/PNG) via multipart/form-data, performs Computer Vision inference, " +
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
            )
    })
    public ResponseEntity<ApiResponse<DiagnosisAnalyzeResponse>> analyzeImage(
            @ModelAttribute @Valid DiagnosisAnalyzeRequest request
    ) {
        DiagnosisAnalyzeResponse response = diagnosisService.analyze(request);
        return ResponseEntity.ok(ApiResponse.success("Image analyzed successfully", response));
    }
}
