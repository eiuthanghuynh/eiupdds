package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service contract for plant leaf disease analysis, Computer Vision inference,
 * dual-coordinate transformation, pathology resolution, and agricultural treatment recommendations.
 */
public interface DiagnosisService {

    /**
     * Executes the end-to-end diagnosis pipeline on an uploaded plant leaf image.
     *
     * @param image         the leaf image file (JPEG, PNG, WebP <= 15MB)
     * @param plantCodeHint optional user/client hint for the plant crop (e.g. "POTATO", "TOMATO")
     * @param latitude      optional GPS latitude coordinate where image was captured
     * @param longitude     optional GPS longitude coordinate where image was captured
     * @param deviceInfo    optional client device identification string
     * @return standardized diagnosis response containing dominant pathology, normalized bounding boxes, and treatments
     */
    DiagnosisAnalyzeResponse analyzeImage(
            MultipartFile image,
            String plantCodeHint,
            Double latitude,
            Double longitude,
            String deviceInfo
    );
}
