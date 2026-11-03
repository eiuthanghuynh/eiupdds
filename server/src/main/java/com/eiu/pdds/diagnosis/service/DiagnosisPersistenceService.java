package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.dto.diagnosis.BoundingBoxDto;
import com.eiu.pdds.plant.entity.Plant;

import java.util.List;

/**
 * Service managing transactional persistence of diagnosis audit trails and bounding boxes (SRP).
 */
public interface DiagnosisPersistenceService {

    /**
     * Persists a completed diagnosis transaction with cascaded bounding boxes.
     */
    Diagnosis saveDiagnosis(
            Plant plant,
            String s3ImageUrl,
            int imageWidth,
            int imageHeight,
            Double latitude,
            Double longitude,
            String deviceInfo,
            Disease dominantDiseaseEntity,
            Double dominantConfidence,
            int inferenceTimeMs,
            String rawAiJson,
            List<BoundingBoxDto> boundingBoxDtos
    );
}
