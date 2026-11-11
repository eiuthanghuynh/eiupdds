package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.diagnosis.entity.BoundingBox;
import com.eiu.pdds.dto.diagnosis.BoundingBoxDto;
import com.eiu.pdds.integration.ai.dto.DetectedObject;
import com.eiu.pdds.integration.roboflow.RoboflowClassMapper;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

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
        return buildBoundingBoxDto(
                detected.detectionId(),
                detected.label(),
                detected.diseaseCode(),
                detected.confidence(),
                detected.x(),
                detected.y(),
                detected.width(),
                detected.height(),
                imgWidth,
                imgHeight
        );
    }

    /**
     * Converts a single persisted BoundingBox entity into a BoundingBoxDto with dual coordinates.
     * Preserves canonical healthy leaf disease code when reconstructing from persistence.
     */
    public BoundingBoxDto toBoundingBoxDto(BoundingBox box, int imgWidth, int imgHeight) {
        String diseaseCode = null;
        if (box.getDisease() != null) {
            diseaseCode = box.getDisease().getDiseaseCode();
        } else if (box.getLabel() != null) {
            diseaseCode = RoboflowClassMapper.fromRawClassName(box.getLabel())
                    .map(RoboflowClassMapper.ClassMetadata::diseaseCode)
                    .orElseGet(() -> {
                        if (box.getDiagnosis() != null && box.getDiagnosis().getPlant() != null) {
                            return box.getDiagnosis().getPlant().getPlantCode() + "_HEALTHY";
                        }
                        return null;
                    });
        }

        return buildBoundingBoxDto(
                box.getId(),
                box.getLabel(),
                diseaseCode,
                box.getConfidence(),
                box.getX(),
                box.getY(),
                box.getWidth(),
                box.getHeight(),
                imgWidth,
                imgHeight
        );
    }

    /**
     * Pure coordinate math calculating normalized top-left bounds and rounded dimensions.
     */
    private BoundingBoxDto buildBoundingBoxDto(
            UUID id,
            String label,
            String diseaseCode,
            double confidence,
            double centerX,
            double centerY,
            double boxWidth,
            double boxHeight,
            int imgWidth,
            int imgHeight
    ) {
        int width = Math.max(1, imgWidth);
        int height = Math.max(1, imgHeight);

        double xMin = centerX - (boxWidth / 2.0);
        double yMin = centerY - (boxHeight / 2.0);

        double xMinNorm = Math.max(0.0, Math.min(1.0, xMin / (double) width));
        double yMinNorm = Math.max(0.0, Math.min(1.0, yMin / (double) height));
        double wNorm = Math.max(0.0, Math.min(1.0, boxWidth / (double) width));
        double hNorm = Math.max(0.0, Math.min(1.0, boxHeight / (double) height));

        return new BoundingBoxDto(
                id,
                label,
                diseaseCode,
                round(confidence, 4),
                round(centerX, 1),
                round(centerY, 1),
                round(boxWidth, 1),
                round(boxHeight, 1),
                round(xMin, 1),
                round(yMin, 1),
                round(xMinNorm, 4),
                round(yMinNorm, 4),
                round(wNorm, 4),
                round(hNorm, 4)
        );
    }

    /**
     * Batch converts a list of persisted BoundingBox entities into BoundingBoxDtos.
     */
    public List<BoundingBoxDto> toBoundingBoxDtosFromEntities(List<BoundingBox> boxes, int imgWidth, int imgHeight) {
        if (boxes == null || boxes.isEmpty()) {
            return Collections.emptyList();
        }
        return boxes.stream()
                .map(box -> toBoundingBoxDto(box, imgWidth, imgHeight))
                .toList();
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
