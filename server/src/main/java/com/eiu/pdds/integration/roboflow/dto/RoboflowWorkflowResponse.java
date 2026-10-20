package com.eiu.pdds.integration.roboflow.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Immutable Java 21 Records modeling the Roboflow Serverless Workflow API response.
 */
public final class RoboflowWorkflowResponse {

    private RoboflowWorkflowResponse() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WorkflowItem(
            @JsonProperty("model_output") ModelOutput modelOutput
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ModelOutput(
            ImageInfo image,
            List<Prediction> predictions
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImageInfo(
            int width,
            int height
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Prediction(
            double x,
            double y,
            double width,
            double height,
            double confidence,
            @JsonProperty("class_id") Integer classId,
            @JsonProperty("class") String className,
            @JsonProperty("detection_id") String detectionId,
            @JsonProperty("parent_id") String parentId
    ) {}
}
