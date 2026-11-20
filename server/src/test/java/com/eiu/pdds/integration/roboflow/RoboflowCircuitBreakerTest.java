package com.eiu.pdds.integration.roboflow;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.integration.roboflow.dto.RoboflowWorkflowResponse;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class RoboflowCircuitBreakerTest {

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
                        "class_id": 12
                      }
                    ]
                  }
                }
              ]
            }
            """;

    private static HttpServer mockRoboflowServer;
    private static final AtomicInteger httpStatusToReturn = new AtomicInteger(200);
    private static final AtomicInteger httpCallCount = new AtomicInteger(0);

    @Autowired
    private RoboflowClient roboflowClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private static synchronized void ensureServerStarted() throws IOException {
        if (mockRoboflowServer != null) {
            return;
        }
        mockRoboflowServer = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        mockRoboflowServer.createContext("/", exchange -> {
            try (InputStream requestBody = exchange.getRequestBody()) {
                requestBody.readAllBytes();
            }
            httpCallCount.incrementAndGet();
            int status = httpStatusToReturn.get();
            byte[] responseBytes = (status >= 200 && status < 300 ? SUCCESS_RESPONSE_JSON : "{\"error\":\"upstream failure\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        });
        mockRoboflowServer.start();
    }

    @DynamicPropertySource
    static void registerRoboflowProperties(DynamicPropertyRegistry registry) throws IOException {
        ensureServerStarted();
        registry.add("roboflow.serverless-url", () -> "http://127.0.0.1:" + mockRoboflowServer.getAddress().getPort());
        registry.add("roboflow.timeout-seconds", () -> "2");
    }

    @AfterAll
    static void stopMockServer() {
        if (mockRoboflowServer != null) {
            mockRoboflowServer.stop(0);
            mockRoboflowServer = null;
        }
    }

    @BeforeEach
    void setUp() {
        circuitBreakerRegistry.circuitBreaker("roboflowInference").reset();
        httpStatusToReturn.set(200);
        httpCallCount.set(0);
    }

    @AfterEach
    void tearDown() {
        circuitBreakerRegistry.circuitBreaker("roboflowInference").reset();
    }

    @Test
    @DisplayName("Client validation error (INVALID_REQUEST_PAYLOAD) does not trip roboflowInference circuit breaker")
    void clientValidationError_doesNotTripCircuitBreaker() {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("roboflowInference");

        for (int i = 0; i < 6; i++) {
            byte[] invalidBytes = (i % 2 == 0) ? null : new byte[0];
            assertThatThrownBy(() -> roboflowClient.inferPlantDisease(invalidBytes, 0.5))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.INVALID_REQUEST_PAYLOAD));
        }

        assertThat(httpCallCount.get()).isZero();
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    @DisplayName("5 upstream failures open circuit and fail fast with 503 AI_002, then recover HALF_OPEN -> CLOSED")
    void upstreamFailures_openCircuitAndFailFast_thenRecoverToClosed() throws InterruptedException {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("roboflowInference");
        byte[] sampleImage = new byte[]{1, 2, 3, 4};

        httpStatusToReturn.set(502);

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> roboflowClient.inferPlantDisease(sampleImage, 0.5))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode())
                            .isEqualTo(ErrorCode.ROBOFLOW_API_ERROR));
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        int callsAfterOpening = httpCallCount.get();
        assertThat(callsAfterOpening).isEqualTo(15); // 5 calls * (1 initial + 2 retries)

        // 6th call must fail fast with 503 AI_002 without hitting downstream HTTP
        assertThatThrownBy(() -> roboflowClient.inferPlantDisease(sampleImage, 0.5))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.AI_SERVICE_UNAVAILABLE);
                    assertThat(be.getErrorCode().getCode()).isEqualTo("AI_002");
                    assertThat(be.getErrorCode().getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                });
        assertThat(httpCallCount.get()).isEqualTo(callsAfterOpening);

        // Wait past waitDurationInOpenState (1s in test profile) for HALF_OPEN transition
        long deadline = System.currentTimeMillis() + 3000L;
        while (circuitBreaker.getState() != CircuitBreaker.State.HALF_OPEN && System.currentTimeMillis() < deadline) {
            Thread.sleep(50L);
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        // Downstream recovers -> 3 permitted HALF_OPEN probe calls succeed and transition breaker back to CLOSED
        httpStatusToReturn.set(200);
        for (int i = 0; i < 3; i++) {
            RoboflowWorkflowResponse.ModelOutput output = roboflowClient.inferPlantDisease(sampleImage, 0.5);
            assertThat(output).isNotNull();
            assertThat(output.predictions()).hasSize(1);
            assertThat(output.predictions().get(0).className()).isEqualTo("Potato Early blight");
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(httpCallCount.get()).isEqualTo(callsAfterOpening + 3);
    }
}
