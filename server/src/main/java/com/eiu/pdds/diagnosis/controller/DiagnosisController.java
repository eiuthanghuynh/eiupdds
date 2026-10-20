package com.eiu.pdds.diagnosis.controller;

import com.eiu.pdds.common.api.ApiResponse;
import com.eiu.pdds.diagnosis.service.DiagnosisMockService;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/diagnoses")
@RequiredArgsConstructor
@Tag(name = "Diagnosis & AI Inference", description = "Endpoints for plant disease detection, leaf image analysis, and treatment protocols")
public class DiagnosisController {

    private final DiagnosisMockService diagnosisMockService;

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
            @Parameter(description = "Leaf photo file (JPEG or PNG, max 15MB)", required = true)
            @RequestParam("image") MultipartFile image,

            @Parameter(description = "Optional crop hint code (e.g., POTATO, TOMATO, RICE)", example = "POTATO")
            @RequestParam(value = "plantCode", required = false) String plantCode,

            @Parameter(description = "GPS Latitude where the photo was captured", example = "11.0528")
            @RequestParam(value = "latitude", required = false) Double latitude,

            @Parameter(description = "GPS Longitude where the photo was captured", example = "106.6661")
            @RequestParam(value = "longitude", required = false) Double longitude,

            @Parameter(description = "Client device information (model, OS version)", example = "iPhone 15 Pro / iOS 17.5")
            @RequestParam(value = "deviceInfo", required = false) String deviceInfo
    ) {
        DiagnosisAnalyzeResponse response = diagnosisMockService.analyzeImage(image, plantCode, latitude, longitude, deviceInfo);
        return ResponseEntity.ok(ApiResponse.success("Image analyzed successfully", response));
    }
}
