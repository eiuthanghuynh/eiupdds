package com.eiu.pdds.integration.ai;

import com.eiu.pdds.integration.ai.dto.InferenceResult;

/**
 * Strategy/Provider interface abstracting leaf pathology computer vision inference (DIP & OCP).
 * Implementations may target Roboflow Serverless, AWS Rekognition, local PyTorch/YOLO,
 * or on-device TFLite/ONNX edge engines (Phase 2).
 */
public interface AiInferenceProvider {

    /**
     * Executes computer vision inference on raw image bytes.
     *
     * @param imageBytes          the raw byte buffer of the plant leaf image
     * @param confidenceThreshold optional minimum confidence threshold
     * @return vendor-agnostic InferenceResult containing image dimensions and normalized detected objects
     */
    InferenceResult inferPlantDisease(byte[] imageBytes, Double confidenceThreshold);
}
