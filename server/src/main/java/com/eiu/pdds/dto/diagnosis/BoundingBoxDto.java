package com.eiu.pdds.dto.diagnosis;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Bounding box detection data transfer object providing dual coordinates:
 * raw pixel dimensions and normalized top-left [0.0 - 1.0] for mobile canvas/SVG scaling.
 */
@Schema(description = "Object detection bounding box with dual coordinate representations (pixel & normalized)")
public record BoundingBoxDto(
        @Schema(description = "Unique identifier of the bounding box detection", example = "31faab02-9bd3-47fe-8258-70e577b27006")
        UUID id,

        @Schema(description = "Detection class label from Computer Vision model", example = "Potato leaf late blight")
        String label,

        @Schema(description = "Standardized system disease code", example = "POTATO_LATE_BLIGHT")
        String diseaseCode,

        @Schema(description = "Model inference confidence score between 0.0 and 1.0", example = "0.7994")
        Double confidence,

        // Raw Pixel Coordinates from Roboflow / Computer Vision model
        @Schema(description = "Center X pixel coordinate on original image", example = "322.0")
        Double x,

        @Schema(description = "Center Y pixel coordinate on original image", example = "426.0")
        Double y,

        @Schema(description = "Bounding box width in pixels", example = "154.0")
        Double width,

        @Schema(description = "Bounding box height in pixels", example = "150.0")
        Double height,

        @Schema(description = "Top-Left X pixel coordinate (x - width/2)", example = "245.0")
        Double xMin,

        @Schema(description = "Top-Left Y pixel coordinate (y - height/2)", example = "351.0")
        Double yMin,

        // Normalized Top-Left Coordinates [0.0 - 1.0] for direct Mobile SVG rendering
        @Schema(description = "Normalized Top-Left X coordinate [0.0 - 1.0] relative to image width", example = "0.6140")
        Double xMinNormalized,

        @Schema(description = "Normalized Top-Left Y coordinate [0.0 - 1.0] relative to image height", example = "0.7006")
        Double yMinNormalized,

        @Schema(description = "Normalized width [0.0 - 1.0] relative to image width", example = "0.3860")
        Double widthNormalized,

        @Schema(description = "Normalized height [0.0 - 1.0] relative to image height", example = "0.2994")
        Double heightNormalized
) {}
