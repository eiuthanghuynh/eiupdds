package com.eiu.pdds.diagnosis.service.impl;

import com.eiu.pdds.diagnosis.entity.BoundingBox;
import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.entity.DiagnosisStatus;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.diagnosis.service.DiagnosisPersistenceService;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.dto.diagnosis.BoundingBoxDto;
import com.eiu.pdds.plant.entity.Plant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Production implementation of DiagnosisPersistenceService (SRP).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisPersistenceServiceImpl implements DiagnosisPersistenceService {

    private final DiagnosisRepository diagnosisRepository;
    private final DiseaseRepository diseaseRepository;

    @Override
    @Transactional
    public Diagnosis saveDiagnosis(
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
        Diagnosis diagnosis = Diagnosis.builder()
                .user(null) // Anonymous farmer in Sprint 2
                .plant(plant)
                .originalImageUrl(s3ImageUrl)
                .imageWidth(imageWidth)
                .imageHeight(imageHeight)
                .latitude(latitude)
                .longitude(longitude)
                .deviceInfo(deviceInfo)
                .dominantDisease(dominantDiseaseEntity)
                .dominantConfidence(dominantConfidence)
                .inferenceTimeMs(inferenceTimeMs)
                .rawAiResponse(rawAiJson)
                .status(DiagnosisStatus.COMPLETED)
                .build();

        if (boundingBoxDtos != null) {
            for (BoundingBoxDto boxDto : boundingBoxDtos) {
                Disease boxDisease = (boxDto.diseaseCode() != null && !boxDto.diseaseCode().endsWith("_HEALTHY"))
                        ? diseaseRepository.findByDiseaseCode(boxDto.diseaseCode()).orElse(null)
                        : null;

                BoundingBox box = BoundingBox.builder()
                        .disease(boxDisease)
                        .label(boxDto.label())
                        .confidence(boxDto.confidence())
                        .x(boxDto.x())
                        .y(boxDto.y())
                        .width(boxDto.width())
                        .height(boxDto.height())
                        .build();

                diagnosis.addBoundingBox(box);
            }
        }

        Diagnosis saved = diagnosisRepository.save(diagnosis);
        log.info("Persisted diagnosis record id='{}' with {} bounding boxes",
                saved.getId(), saved.getBoundingBoxes().size());

        return saved;
    }
}
