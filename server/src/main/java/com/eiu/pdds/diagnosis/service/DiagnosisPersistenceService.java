package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.dto.diagnosis.BoundingBoxDto;
import com.eiu.pdds.plant.entity.Plant;

import java.util.List;
import java.util.UUID;

/**
 * Service managing transactional persistence of diagnosis audit trails and bounding boxes (SRP).
 */
public interface DiagnosisPersistenceService {

    /**
     * Persists a completed diagnosis transaction with cascaded bounding boxes and linked user.
     */
    Diagnosis saveDiagnosis(
            UUID userId,
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

    default Diagnosis saveDiagnosis(
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
    ) {
        return saveDiagnosis(
                null,
                plant,
                s3ImageUrl,
                imageWidth,
                imageHeight,
                latitude,
                longitude,
                deviceInfo,
                dominantDiseaseEntity,
                dominantConfidence,
                inferenceTimeMs,
                rawAiJson,
                boundingBoxDtos
        );
    }
}
