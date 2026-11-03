package com.eiu.pdds.integration.ai.dto;

import java.util.UUID;

/**
 * Domain-level immutable representation of an object detected by Computer Vision models.
 * Completely vendor-agnostic (does not depend on Roboflow, YOLO, or ONNX specific structures).
 * Contains domain-mapped diseaseCode, plantCode, and health status resolved by the AI Adapter (Anti-Corruption Layer).
 */
public record DetectedObject(
        UUID detectionId,
        String label,
        String diseaseCode,
        String plantCode,
        String englishName,
        boolean isHealthy,
        double confidence,
        double x,
        double y,
        double width,
        double height
) {
    /**
     * Backward-compatible convenience constructor for tests with raw labels.
     */
    public DetectedObject(
            UUID detectionId,
            String label,
            double confidence,
            double x,
            double y,
            double width,
            double height
    ) {
        this(detectionId, label, label, null, label, false, confidence, x, y, width, height);
    }
}
