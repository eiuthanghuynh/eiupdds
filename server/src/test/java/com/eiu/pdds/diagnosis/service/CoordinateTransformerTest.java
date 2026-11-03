package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.dto.diagnosis.BoundingBoxDto;
import com.eiu.pdds.integration.ai.dto.DetectedObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CoordinateTransformerTest {

    private CoordinateTransformer transformer;

    @BeforeEach
    void setUp() {
        transformer = new CoordinateTransformer();
    }

    @Test
    @DisplayName("Should transform DetectedObject into dual-coordinate BoundingBoxDto correctly")
    void testToBoundingBoxDto_Success() {
        UUID id = UUID.randomUUID();
        // Center x=322, y=426, w=154, h=150 on image 399 x 501
        DetectedObject detected = new DetectedObject(
                id,
                "Potato leaf late blight",
                "POTATO_LATE_BLIGHT",
                "POTATO",
                "Potato late blight",
                false,
                0.799439,
                322.0, 426.0, 154.0, 150.0
        );

        BoundingBoxDto dto = transformer.toBoundingBoxDto(detected, 399, 501);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.label()).isEqualTo("Potato leaf late blight");
        assertThat(dto.diseaseCode()).isEqualTo("POTATO_LATE_BLIGHT");
        assertThat(dto.confidence()).isEqualTo(0.7994);

        // Pixel center
        assertThat(dto.x()).isEqualTo(322.0);
        assertThat(dto.y()).isEqualTo(426.0);
        assertThat(dto.width()).isEqualTo(154.0);
        assertThat(dto.height()).isEqualTo(150.0);

        // xMin = 322 - 77 = 245.0, yMin = 426 - 75 = 351.0
        assertThat(dto.xMin()).isEqualTo(245.0);
        assertThat(dto.yMin()).isEqualTo(351.0);

        // Normalized: 245 / 399 = 0.614035... -> 0.6140
        assertThat(dto.xMinNormalized()).isBetween(0.61, 0.62);
        assertThat(dto.yMinNormalized()).isBetween(0.70, 0.71);
        assertThat(dto.widthNormalized()).isBetween(0.38, 0.39);
        assertThat(dto.heightNormalized()).isBetween(0.29, 0.30);
    }

    @Test
    @DisplayName("Should clamp normalized coordinates within [0.0, 1.0] when detection extends beyond bounds")
    void testToBoundingBoxDto_Clamping() {
        UUID id = UUID.randomUUID();
        DetectedObject outOfBounds = new DetectedObject(
                id,
                "Tomato leaf",
                "TOMATO_HEALTHY",
                "TOMATO",
                "Healthy tomato leaf",
                true,
                0.95,
                20.0, 20.0, 100.0, 100.0
        );

        BoundingBoxDto dto = transformer.toBoundingBoxDto(outOfBounds, 200, 200);

        // xMin = 20 - 50 = -30 -> clamped to 0.0
        assertThat(dto.xMinNormalized()).isEqualTo(0.0);
        assertThat(dto.yMinNormalized()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should batch transform list of detections")
    void testToBoundingBoxDtos_Batch() {
        List<DetectedObject> detections = List.of(
                new DetectedObject(UUID.randomUUID(), "Tomato leaf", "TOMATO_HEALTHY", "TOMATO", "Healthy tomato leaf", true, 0.90, 50.0, 50.0, 40.0, 40.0),
                new DetectedObject(UUID.randomUUID(), "Corn rust leaf", "CORN_COMMON_RUST", "CORN", "Corn common rust", false, 0.85, 80.0, 80.0, 30.0, 30.0)
        );

        List<BoundingBoxDto> dtos = transformer.toBoundingBoxDtos(detections, 100, 100);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).diseaseCode()).isEqualTo("TOMATO_HEALTHY");
        assertThat(dtos.get(1).diseaseCode()).isEqualTo("CORN_COMMON_RUST");
    }
}
