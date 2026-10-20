package com.eiu.pdds.integration.roboflow;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.integration.roboflow.dto.RoboflowWorkflowResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * High-performance Roboflow Serverless Workflow client built on Java 21 RestClient.
 * Fully compatible with Virtual Threads (Project Loom) via JdkClientHttpRequestFactory.
 */
@Slf4j
@Component
public class RoboflowClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${roboflow.api-key:}")
    private String apiKey;

    @Value("${roboflow.workspace:plant-disease-detection-mztfb}")
    private String workspace;

    @Value("${roboflow.workflow-id:7BrZ3OIi8ZsxxlqKtKm1}")
    private String workflowId;

    @Value("${roboflow.serverless-url:https://serverless.roboflow.com}")
    private String serverlessUrl;

    @Value("${roboflow.confidence-threshold:0.4}")
    private double defaultConfidenceThreshold;

    @org.springframework.beans.factory.annotation.Autowired
    public RoboflowClient(
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            @Value("${roboflow.timeout-seconds:15}") int timeoutSeconds
    ) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Overloaded constructor for unit testing with custom RestClient.
     */
    public RoboflowClient(
            RestClient restClient,
            ObjectMapper objectMapper,
            String apiKey,
            String workspace,
            String workflowId,
            String serverlessUrl,
            double defaultConfidenceThreshold
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.workspace = workspace;
        this.workflowId = workflowId;
        this.serverlessUrl = serverlessUrl;
        this.defaultConfidenceThreshold = defaultConfidenceThreshold;
    }

    /**
     * Executes serverless workflow inference on a plant leaf image with smart exponential backoff retry.
     *
     * @param imageBytes          raw bytes of the leaf photo
     * @param confidenceThreshold minimum confidence threshold (falls back to default 0.4 if null)
     * @return parsed ModelOutput containing image dimensions and predictions
     */
    public RoboflowWorkflowResponse.ModelOutput inferPlantDisease(byte[] imageBytes, Double confidenceThreshold) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "Image byte content cannot be empty for inference");
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("ROBOFLOW_API_KEY is not configured. Real inference cannot be performed.");
            throw new BusinessException(ErrorCode.ROBOFLOW_API_ERROR, "Roboflow API key is not configured on this server");
        }

        double confidence = (confidenceThreshold != null && confidenceThreshold > 0.0)
                ? confidenceThreshold
                : defaultConfidenceThreshold;

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> payload = Map.of(
                "api_key", apiKey,
                "inputs", Map.of(
                        "image", Map.of(
                                "type", "base64",
                                "value", base64Image
                        )
                ),
                "parameters", Map.of(
                        "confidence", confidence
                )
        );

        String targetUrl = String.format("%s/%s/workflows/%s", serverlessUrl, workspace, workflowId);
        int maxRetries = 2;
        int attempt = 0;
        Exception lastException = null;

        while (attempt <= maxRetries) {
            try {
                log.info("Sending image to Roboflow Serverless Workflow: url={}, attempt={}/{}", targetUrl, attempt + 1, maxRetries + 1);

                String responseBody = restClient.post()
                        .uri(targetUrl)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                        .body(payload)
                        .retrieve()
                        .body(String.class);

                return parseWorkflowResponse(responseBody);

            } catch (HttpClientErrorException clientEx) {
                // Client error (4xx): Invalid key, bad request -> DO NOT RETRY
                log.error("Roboflow client error [HTTP {}]: {}", clientEx.getStatusCode(), clientEx.getResponseBodyAsString());
                throw new BusinessException(ErrorCode.ROBOFLOW_API_ERROR, "Roboflow client error: " + clientEx.getMessage());

            } catch (HttpServerErrorException serverEx) {
                // Server error (5xx): Service unavailable, gateway timeout -> RETRY
                log.warn("Roboflow server error [HTTP {}] on attempt {}: {}", serverEx.getStatusCode(), attempt + 1, serverEx.getMessage());
                lastException = serverEx;

            } catch (ResourceAccessException timeoutEx) {
                // Timeout / network error -> RETRY
                log.warn("Roboflow connection timeout on attempt {}: {}", attempt + 1, timeoutEx.getMessage());
                lastException = timeoutEx;

            } catch (Exception ex) {
                log.error("Unexpected error during Roboflow workflow execution: ", ex);
                throw new BusinessException(ErrorCode.ROBOFLOW_API_ERROR, "Unexpected error communicating with Roboflow: " + ex.getMessage());
            }

            attempt++;
            if (attempt <= maxRetries) {
                long backoffMs = (long) Math.pow(2, attempt) * 1000L;
                log.info("Backing off for {} ms before next retry...", backoffMs);
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Retry interrupted");
                }
            }
        }

        throw new BusinessException(ErrorCode.ROBOFLOW_API_ERROR,
                "Roboflow workflow failed after " + (maxRetries + 1) + " attempts. Last error: " + (lastException != null ? lastException.getMessage() : "Unknown"));
    }

    private RoboflowWorkflowResponse.ModelOutput parseWorkflowResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new BusinessException(ErrorCode.ROBOFLOW_API_ERROR, "Received empty response body from Roboflow workflow");
        }

        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode itemsNode = rootNode.has("outputs") ? rootNode.get("outputs") : rootNode;

            List<RoboflowWorkflowResponse.WorkflowItem> items = objectMapper.convertValue(
                    itemsNode,
                    new TypeReference<>() {}
            );

            if (items == null || items.isEmpty() || items.get(0).modelOutput() == null) {
                throw new BusinessException(ErrorCode.ROBOFLOW_API_ERROR, "Roboflow workflow output does not contain model predictions");
            }

            return items.get(0).modelOutput();

        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Failed to parse Roboflow workflow response JSON: payload={}", responseBody, e);
            throw new BusinessException(ErrorCode.ROBOFLOW_API_ERROR, "Failed to parse Roboflow predictions: " + e.getMessage());
        }
    }
}
