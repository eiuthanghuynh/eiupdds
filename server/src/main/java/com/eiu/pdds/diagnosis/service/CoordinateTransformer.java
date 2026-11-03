package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.dto.diagnosis.BoundingBoxDto;
import com.eiu.pdds.integration.ai.dto.DetectedObject;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Pure mathematical component responsible for geometric transformations of bounding boxes (SRP).
 * Converts center-based pixel coordinates to top-left normalized coordinates [0.0 - 1.0].
 * Completely decoupled from AI vendor classes.
 */
@Component
public class CoordinateTransformer {

    /**
     * Converts a single DetectedObject into a BoundingBoxDto with dual coordinates.
     */
    public BoundingBoxDto toBoundingBoxDto(DetectedObject detected, int imgWidth, int imgHeight) {
        int width = Math.max(1, imgWidth);
        int height = Math.max(1, imgHeight);

        double xMin = detected.x() - (detected.width() / 2.0);
        double yMin = detected.y() - (detected.height() / 2.0);

        double xMinNorm = Math.max(0.0, Math.min(1.0, xMin / (double) width));
        double yMinNorm = Math.max(0.0, Math.min(1.0, yMin / (double) height));
        double wNorm = Math.max(0.0, Math.min(1.0, detected.width() / (double) width));
        double hNorm = Math.max(0.0, Math.min(1.0, detected.height() / (double) height));

        return new BoundingBoxDto(
                detected.detectionId(),
                detected.label(),
                detected.diseaseCode(),
                round(detected.confidence(), 4),
                round(detected.x(), 1),
                round(detected.y(), 1),
                round(detected.width(), 1),
                round(detected.height(), 1),
                round(xMin, 1),
                round(yMin, 1),
                round(xMinNorm, 4),
                round(yMinNorm, 4),
                round(wNorm, 4),
                round(hNorm, 4)
        );
    }

    /**
     * Batch converts a list of DetectedObjects into BoundingBoxDtos.
     */
    public List<BoundingBoxDto> toBoundingBoxDtos(List<DetectedObject> detections, int imgWidth, int imgHeight) {
        if (detections == null || detections.isEmpty()) {
            return Collections.emptyList();
        }
        return detections.stream()
                .map(d -> toBoundingBoxDto(d, imgWidth, imgHeight))
                .toList();
    }

    public static double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }
}
