package com.eiu.pdds.integration.roboflow;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.common.util.FileSecurityValidator;
import com.eiu.pdds.integration.ai.AiInferenceProvider;
import com.eiu.pdds.integration.ai.dto.DetectedObject;
import com.eiu.pdds.integration.ai.dto.InferenceResult;
import com.eiu.pdds.integration.roboflow.dto.RoboflowWorkflowResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Adapter implementing AiInferenceProvider by delegating to RoboflowClient.
 * Acts as an Anti-Corruption Layer (ACL), translating Roboflow proprietary workflow DTOs
 * and model class names into standardized domain-level InferenceResult (DIP & OCP).
 */
@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class RoboflowInferenceProvider implements AiInferenceProvider {

    private final RoboflowClient roboflowClient;
    private final RoboflowClassMapper classMapper;
    private final ObjectMapper objectMapper;

    @Override
    public InferenceResult inferPlantDisease(byte[] imageBytes, Double confidenceThreshold) {
        RoboflowWorkflowResponse.ModelOutput output = roboflowClient.inferPlantDisease(imageBytes, confidenceThreshold);

        int imageWidth = (output.image() != null) ? output.image().width() : 0;
        int imageHeight = (output.image() != null) ? output.image().height() : 0;

        // Enterprise Resolution (Source of Truth & Fail-Fast):
        // If AI provider metadata is missing or non-positive, inspect source image bytes directly.
        // If dimensions cannot be resolved from either, fail fast immediately to avoid aspect ratio corruption.
        if (imageWidth <= 0 || imageHeight <= 0) {
            log.warn("Roboflow response missing valid image dimensions (width={}, height={}). Inspecting source image bytes.",
                    imageWidth, imageHeight);
            FileSecurityValidator.ImageDimensions dims = FileSecurityValidator.extractDimensions(imageBytes)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.ROBOFLOW_API_ERROR,
                            "Roboflow returned invalid image dimensions and unable to extract dimensions from source image bytes"
                    ));
            imageWidth = dims.width();
            imageHeight = dims.height();
            log.info("Resolved image dimensions from source image bytes: {}x{}", imageWidth, imageHeight);
        }

        List<DetectedObject> detections = (output.predictions() == null)
                ? List.of()
                : output.predictions().stream()
                .map(this::toDetectedObject)
                .toList();

        String rawPayloadJson = serializeRawOutput(output);

        return new InferenceResult(imageWidth, imageHeight, detections, rawPayloadJson);
    }

    private DetectedObject toDetectedObject(RoboflowWorkflowResponse.Prediction p) {
        UUID detectionId = parseUuidSafely(p.detectionId());
        String label = p.className();

        RoboflowClassMapper.ClassMetadata meta = classMapper.resolve(label).orElse(null);
        String diseaseCode = classMapper.resolveDiseaseCode(label);
        String plantCode = (meta != null) ? meta.plantCode() : classMapper.resolvePlantCode(label);
        String englishName = (meta != null) ? meta.englishName() : classMapper.resolveEnglishName(label);
        boolean isHealthy = (meta != null) && meta.isHealthy();

        return new DetectedObject(
                detectionId,
                label,
                diseaseCode,
                plantCode,
                englishName,
                isHealthy,
                p.confidence(),
                p.x(),
                p.y(),
                p.width(),
                p.height()
        );
    }

    private UUID parseUuidSafely(String str) {
        if (str == null || str.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(str);
        } catch (IllegalArgumentException e) {
            return UUID.randomUUID();
        }
    }

    private String serializeRawOutput(RoboflowWorkflowResponse.ModelOutput output) {
        try {
            return objectMapper.writeValueAsString(output);
        } catch (Exception e) {
            log.warn("Failed to serialize Roboflow ModelOutput to JSON: ", e);
            return "{}";
        }
    }
}
