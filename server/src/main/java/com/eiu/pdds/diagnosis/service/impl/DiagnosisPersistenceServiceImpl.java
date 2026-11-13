package com.eiu.pdds.diagnosis.service.impl;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.diagnosis.dto.DiagnosisSaveCommand;
import com.eiu.pdds.diagnosis.entity.BoundingBox;
import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.entity.DiagnosisStatus;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.diagnosis.service.DiagnosisPersistenceService;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.dto.diagnosis.BoundingBoxDto;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Production implementation of DiagnosisPersistenceService (SRP).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisPersistenceServiceImpl implements DiagnosisPersistenceService {

    private final DiagnosisRepository diagnosisRepository;
    private final DiseaseRepository diseaseRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Diagnosis saveDiagnosis(DiagnosisSaveCommand command) {
        if (command == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "DiagnosisSaveCommand must not be null");
        }

        User user = command.userId() != null ? userRepository.getReferenceById(command.userId()) : null;

        Diagnosis diagnosis = Diagnosis.builder()
                .user(user)
                .plant(command.plant())
                .originalImageUrl(command.s3ImageUrl())
                .imageWidth(command.imageWidth())
                .imageHeight(command.imageHeight())
                .latitude(command.latitude())
                .longitude(command.longitude())
                .deviceInfo(command.deviceInfo())
                .dominantDisease(command.dominantDisease())
                .dominantConfidence(command.dominantConfidence())
                .inferenceTimeMs(command.inferenceTimeMs())
                .rawAiResponse(command.rawAiJson() != null ? command.rawAiJson() : "{}")
                .status(DiagnosisStatus.COMPLETED)
                .build();

        if (command.boundingBoxes() != null && !command.boundingBoxes().isEmpty()) {
            Set<String> diseaseCodes = command.boundingBoxes().stream()
                    .map(BoundingBoxDto::diseaseCode)
                    .filter(code -> code != null && !code.endsWith("_HEALTHY"))
                    .collect(Collectors.toSet());

            Map<String, Disease> diseaseMap = diseaseCodes.isEmpty() ? Collections.emptyMap() :
                    diseaseRepository.findByDiseaseCodeIn(diseaseCodes).stream()
                            .collect(Collectors.toMap(Disease::getDiseaseCode, d -> d, (d1, d2) -> d1));

            for (BoundingBoxDto boxDto : command.boundingBoxes()) {
                Disease boxDisease = (boxDto.diseaseCode() != null && !boxDto.diseaseCode().endsWith("_HEALTHY"))
                        ? diseaseMap.get(boxDto.diseaseCode())
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

    @Override
    @Transactional
    public Diagnosis saveDiagnosis(
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
}
