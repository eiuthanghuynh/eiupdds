package com.eiu.pdds.integration.ai.dto;

import java.util.List;

/**
 * Domain-level immutable result of a computer vision leaf disease inference operation.
 */
public record InferenceResult(
        int imageWidth,
        int imageHeight,
        List<DetectedObject> detections,
        String rawPayloadJson
) {
    public InferenceResult {
        if (detections == null) {
            detections = List.of();
        }
        if (rawPayloadJson == null) {
            rawPayloadJson = "{}";
        }
    }

    public boolean hasDetections() {
        return !detections.isEmpty();
    }
}
