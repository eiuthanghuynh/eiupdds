package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.diagnosis.service.dto.PathologyResolution;
import com.eiu.pdds.integration.ai.dto.DetectedObject;

import java.util.List;

/**
 * Service interface responsible for identifying dominant pathologies,
 * verifying catalog registration, detecting co-infections, and resolving plant crops (SRP).
 */
public interface PathologyAnalysisService {

    /**
     * Analyzes detected objects, resolves dominant pathology by max confidence,
     * checks for healthy foliage, and maps co-occurring secondary pathologies.
     *
     * @param detections    detected objects from computer vision inference
     * @param plantCodeHint optional user-provided crop hint code
     * @return structured PathologyResolution containing dominant pathology, healthy flag, and co-occurrences
     */
    PathologyResolution resolvePathology(List<DetectedObject> detections, String plantCodeHint);
}
