package com.eiu.pdds;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.integration.roboflow.RoboflowClient;
import com.eiu.pdds.integration.roboflow.dto.RoboflowWorkflowResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RoboflowClientTests {

    private MockRestServiceServer mockServer;
    private RoboflowClient roboflowClient;
    private ObjectMapper objectMapper;

    private static final String TARGET_URL = "https://serverless.roboflow.com/plant-disease-detection-mztfb/workflows/7BrZ3OIi8ZsxxlqKtKm1";
    private static final String MOCK_API_KEY = "test-rf-api-key";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        roboflowClient = new RoboflowClient(
                restClient,
                objectMapper,
                MOCK_API_KEY,
                "plant-disease-detection-mztfb",
                "7BrZ3OIi8ZsxxlqKtKm1",
                "https://serverless.roboflow.com",
                0.4
        );
    }

    @Test
    @DisplayName("Should successfully send image to Roboflow workflow and parse detections (outputs envelope)")
    void testSuccessfulInference() {
        String mockResponseBody = """
                {
                  "outputs": [
                    {
                      "model_output": {
                        "image": {
                          "width": 399,
                          "height": 501
                        },
                        "predictions": [
                          {
                            "width": 154.0,
                            "height": 150.0,
                            "x": 322.0,
                            "y": 426.0,
                            "confidence": 0.7994,
                            "class_id": 13,
                            "class": "Potato leaf late blight",
                            "detection_id": "31faab02-9bd3-47fe-8258-70e577b27006",
                            "parent_id": "image"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo(TARGET_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + MOCK_API_KEY))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        byte[] fakeImageBytes = new byte[]{1, 2, 3, 4};
        RoboflowWorkflowResponse.ModelOutput output = roboflowClient.inferPlantDisease(fakeImageBytes, 0.5);

        mockServer.verify();
        assertThat(output).isNotNull();
        assertThat(output.image().width()).isEqualTo(399);
        assertThat(output.image().height()).isEqualTo(501);
        assertThat(output.predictions()).hasSize(1);

        RoboflowWorkflowResponse.Prediction pred = output.predictions().get(0);
        assertThat(pred.className()).isEqualTo("Potato leaf late blight");
        assertThat(pred.confidence()).isEqualTo(0.7994);
        assertThat(pred.classId()).isEqualTo(13);
    }

    @Test
    @DisplayName("Should support legacy direct array format from Roboflow workflow")
    void testSuccessfulInferenceLegacyArrayFormat() {
        String mockResponseBody = """
                [
                  {
                    "model_output": {
                      "image": {
                        "width": 399,
                        "height": 501
                      },
                      "predictions": [
                        {
                          "width": 154.0,
                          "height": 150.0,
                          "x": 322.0,
                          "y": 426.0,
                          "confidence": 0.7994,
                          "class_id": 13,
                          "class": "Potato leaf late blight",
                          "detection_id": "31faab02-9bd3-47fe-8258-70e577b27006",
                          "parent_id": "image"
                        }
                      ]
                    }
                  }
                ]
                """;

        mockServer.expect(requestTo(TARGET_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + MOCK_API_KEY))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(mockResponseBody, MediaType.APPLICATION_JSON));

        byte[] fakeImageBytes = new byte[]{1, 2, 3, 4};
        RoboflowWorkflowResponse.ModelOutput output = roboflowClient.inferPlantDisease(fakeImageBytes, 0.5);

        mockServer.verify();
        assertThat(output).isNotNull();
        assertThat(output.predictions()).hasSize(1);
    }

    @Test
    @DisplayName("Should reject and fail fast without retry on 4xx client errors")
    void testClientErrorFailsFastWithoutRetry() {
        mockServer.expect(requestTo(TARGET_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"error\": \"Invalid API Key\"}"));

        byte[] fakeImageBytes = new byte[]{1, 2, 3, 4};

        assertThatThrownBy(() -> roboflowClient.inferPlantDisease(fakeImageBytes, 0.4))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.ROBOFLOW_API_ERROR));

        // Verifies mockServer was only called once (no retries attempted on 4xx)
        mockServer.verify();
    }
}
