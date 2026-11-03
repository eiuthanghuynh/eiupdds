package com.eiu.pdds.integration.roboflow;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.integration.ai.dto.DetectedObject;
import com.eiu.pdds.integration.ai.dto.InferenceResult;
import com.eiu.pdds.integration.roboflow.dto.RoboflowWorkflowResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoboflowInferenceProviderTest {

    @Mock
    private RoboflowClient roboflowClient;

    private RoboflowInferenceProvider provider;

    @BeforeEach
    void setUp() {
        provider = new RoboflowInferenceProvider(roboflowClient, new RoboflowClassMapper(), new ObjectMapper());
    }

    @Test
    @DisplayName("Should adapt Roboflow ModelOutput to domain InferenceResult and resolve domain metadata (ACL)")
    void testInferPlantDisease_Success() {
        byte[] dummyBytes = new byte[]{1, 2, 3};
        RoboflowWorkflowResponse.ModelOutput mockOutput = new RoboflowWorkflowResponse.ModelOutput(
                new RoboflowWorkflowResponse.ImageInfo(399, 501),
                List.of(
                        new RoboflowWorkflowResponse.Prediction(322.0, 426.0, 154.0, 150.0, 0.7994, 13, "Potato leaf late blight", "31faab02-9bd3-47fe-8258-70e577b27006", "image")
                )
        );

        when(roboflowClient.inferPlantDisease(dummyBytes, 0.4)).thenReturn(mockOutput);

        InferenceResult result = provider.inferPlantDisease(dummyBytes, 0.4);

        assertThat(result).isNotNull();
        assertThat(result.imageWidth()).isEqualTo(399);
        assertThat(result.imageHeight()).isEqualTo(501);
        assertThat(result.detections()).hasSize(1);

        DetectedObject detection = result.detections().get(0);
        assertThat(detection.label()).isEqualTo("Potato leaf late blight");
        assertThat(detection.diseaseCode()).isEqualTo("POTATO_LATE_BLIGHT");
        assertThat(detection.plantCode()).isEqualTo("POTATO");
        assertThat(detection.englishName()).isEqualTo("Potato late blight");
        assertThat(detection.isHealthy()).isFalse();
        assertThat(detection.confidence()).isEqualTo(0.7994);
    }

    @Test
    @DisplayName("Should fallback to source image bytes when Roboflow response misses image dimensions (Source of Truth)")
    void testInferPlantDisease_FallbackToSourceImageBytes() throws IOException {
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] realPngBytes = baos.toByteArray();

        RoboflowWorkflowResponse.ModelOutput mockOutputWithoutDims = new RoboflowWorkflowResponse.ModelOutput(
                null, // Missing image dimensions from Roboflow
                List.of(
                        new RoboflowWorkflowResponse.Prediction(400.0, 300.0, 100.0, 100.0, 0.85, 1, "Tomato leaf", "f8b7e2a1-0000-0000-0000-000000000000", "image")
                )
        );

        when(roboflowClient.inferPlantDisease(realPngBytes, 0.4)).thenReturn(mockOutputWithoutDims);

        InferenceResult result = provider.inferPlantDisease(realPngBytes, 0.4);

        assertThat(result).isNotNull();
        assertThat(result.imageWidth()).isEqualTo(800);
        assertThat(result.imageHeight()).isEqualTo(600);
        assertThat(result.detections()).hasSize(1);
        assertThat(result.detections().get(0).label()).isEqualTo("Tomato leaf");
        assertThat(result.detections().get(0).plantCode()).isEqualTo("TOMATO");
    }

    @Test
    @DisplayName("Should Fail-Fast when neither Roboflow nor source image bytes provide valid dimensions")
    void testInferPlantDisease_FailsFast_WhenDimensionsUnresolvable() {
        byte[] unreadableBytes = new byte[]{1, 2, 3}; // Not an image header
        RoboflowWorkflowResponse.ModelOutput mockOutputWithZeroDims = new RoboflowWorkflowResponse.ModelOutput(
                new RoboflowWorkflowResponse.ImageInfo(0, 0),
                List.of()
        );

        when(roboflowClient.inferPlantDisease(unreadableBytes, null)).thenReturn(mockOutputWithZeroDims);

        assertThatThrownBy(() -> provider.inferPlantDisease(unreadableBytes, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ROBOFLOW_API_ERROR);
                    assertThat(be.getMessage()).contains("Roboflow returned invalid image dimensions");
                });
    }
}
