package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.diagnosis.dto.DiagnosisSaveCommand;
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
     * Primary method: Persists a completed diagnosis transaction using an encapsulated parameter object.
     *
     * @param command parameter object containing diagnosis metadata, leaf image attributes, and detected boxes
     * @return persisted Diagnosis entity
     */
    Diagnosis saveDiagnosis(DiagnosisSaveCommand command);

    /**
     * Backwards-compatible overload wrapping individual arguments into DiagnosisSaveCommand.
     */
    default Diagnosis saveDiagnosis(
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
    ) {
        return saveDiagnosis(new DiagnosisSaveCommand(
                userId,
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
        ));
    }

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
