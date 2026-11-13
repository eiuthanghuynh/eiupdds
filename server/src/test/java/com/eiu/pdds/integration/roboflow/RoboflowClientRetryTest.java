package com.eiu.pdds.integration.roboflow;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RoboflowClientRetryTest {

    private RoboflowClient roboflowClient;
    private MockRestServiceServer mockServer;

    private static final String TARGET_URL = "https://serverless.roboflow.com/test-workspace/workflows/test-workflow";
    private static final String SUCCESS_RESPONSE_JSON = """
            {
                "outputs": [
                    {
                        "model_output": {
                            "image": { "width": 400, "height": 300 },
                            "predictions": [
                                {
                                    "x": 100.0,
                                    "y": 150.0,
                                    "width": 50.0,
                                    "height": 60.0,
                                    "confidence": 0.88,
                                    "class": "Potato Early blight",
                                    "class_id": 1
                                }
                            ]
                        }
                    }
                ]
            }
            """;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        roboflowClient = new RoboflowClient(
                restClient,
                objectMapper,
                "test-api-key",
                "test-workspace",
                "test-workflow",
                "https://serverless.roboflow.com",
                0.4
        );
    }

    @Test
    @DisplayName("inferPlantDisease - Retries on HTTP 429 Too Many Requests and succeeds on subsequent attempt")
    void shouldRetryOn429AndSucceed() {
        // Attempt 1: 429 Too Many Requests with Retry-After header: 1
        mockServer.expect(requestTo(TARGET_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", "1")
                        .body("Rate limit exceeded"));

        // Attempt 2: 200 OK
        mockServer.expect(requestTo(TARGET_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andRespond(withSuccess(SUCCESS_RESPONSE_JSON, MediaType.APPLICATION_JSON));

        byte[] sampleImage = new byte[]{1, 2, 3};
        RoboflowWorkflowResponse.ModelOutput response = roboflowClient.inferPlantDisease(sampleImage, 0.5);

        mockServer.verify();
        assertThat(response).isNotNull();
        List<RoboflowWorkflowResponse.Prediction> predictions = response.predictions();
        assertThat(predictions).hasSize(1);
        assertThat(predictions.get(0).className()).isEqualTo("Potato Early blight");
    }

    @Test
    @DisplayName("inferPlantDisease - Exceeding max retries on persistent HTTP 429 throws BusinessException")
    void shouldThrowBusinessExceptionWhen429ExceedsMaxRetries() {
        // 3 consecutive 429 responses (attempt 0, attempt 1, attempt 2)
        for (int i = 0; i < 3; i++) {
            mockServer.expect(requestTo(TARGET_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                            .body("Rate limit exceeded"));
        }

        byte[] sampleImage = new byte[]{1, 2, 3};
        assertThatThrownBy(() -> roboflowClient.inferPlantDisease(sampleImage, 0.5))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ROBOFLOW_API_ERROR);
                });

        mockServer.verify();
    }

    @Test
    @DisplayName("inferPlantDisease - Non-retryable 4xx error (e.g. 401 Unauthorized) fails fast without retry")
    void shouldFailFastOnNonRetryable4xx() {
        mockServer.expect(requestTo(TARGET_URL))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .body("Invalid API key"));

        byte[] sampleImage = new byte[]{1, 2, 3};
        assertThatThrownBy(() -> roboflowClient.inferPlantDisease(sampleImage, 0.5))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.ROBOFLOW_API_ERROR);
                });

        mockServer.verify();
    }
}
