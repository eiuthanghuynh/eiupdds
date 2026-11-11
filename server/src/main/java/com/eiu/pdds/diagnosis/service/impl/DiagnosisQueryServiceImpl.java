package com.eiu.pdds.diagnosis.service.impl;

import com.eiu.pdds.common.api.PageResponse;
import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.diagnosis.entity.BoundingBox;
import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.repository.BoundingBoxRepository;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.diagnosis.repository.DiagnosisSpecifications;
import com.eiu.pdds.diagnosis.service.CoordinateTransformer;
import com.eiu.pdds.diagnosis.service.DiagnosisIdempotencyService;
import com.eiu.pdds.diagnosis.service.DiagnosisQueryService;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.dto.diagnosis.BoundingBoxDto;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import com.eiu.pdds.dto.diagnosis.DiagnosisHistoryFilter;
import com.eiu.pdds.dto.diagnosis.DiagnosisSummaryDto;
import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import com.eiu.pdds.dto.plant.PlantDto;
import com.eiu.pdds.dto.treatment.TreatmentDto;
import com.eiu.pdds.integration.roboflow.RoboflowClassMapper;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.treatment.service.TreatmentRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Production implementation of DiagnosisQueryService (SRP, DIP, CQRS-lite).
 * Specializes in user-isolated read operations, N+1 query elimination, and coordinate reconstruction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiagnosisQueryServiceImpl implements DiagnosisQueryService {

    private final DiagnosisRepository diagnosisRepository;
    private final BoundingBoxRepository boundingBoxRepository;
    private final CoordinateTransformer coordinateTransformer;
    private final TreatmentRecommendationService treatmentRecommendationService;
    private final DiagnosisIdempotencyService idempotencyService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DiagnosisSummaryDto> getUserHistory(UUID userId, DiagnosisHistoryFilter filter, Pageable pageable) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "Authenticated user ID is required");
        }

        Specification<Diagnosis> spec = DiagnosisSpecifications.withFilter(userId, filter);
        Page<Diagnosis> page = diagnosisRepository.findAll(spec, pageable);

        if (page.isEmpty()) {
            return PageResponse.of(Page.empty(pageable));
        }

        List<UUID> diagnosisIds = page.getContent().stream()
                .map(Diagnosis::getId)
                .toList();

        List<Object[]> boxCounts = boundingBoxRepository.countByDiagnosisIds(diagnosisIds);
        Map<UUID, Integer> countMap = boxCounts.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).intValue()
                ));

        Page<DiagnosisSummaryDto> summaryPage = page.map(diagnosis -> toSummaryDto(diagnosis, countMap.getOrDefault(diagnosis.getId(), 0)));
        return PageResponse.of(summaryPage);
    }

    @Override
    @Transactional(readOnly = true)
    public DiagnosisAnalyzeResponse getUserDiagnosisDetail(UUID userId, UUID diagnosisId) {
        if (userId == null || diagnosisId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "User ID and Diagnosis ID are required");
        }

        Diagnosis diagnosis = diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull(diagnosisId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND, "Diagnosis session not found or access denied"));

        int imgWidth = diagnosis.getImageWidth() != null ? diagnosis.getImageWidth() : 0;
        int imgHeight = diagnosis.getImageHeight() != null ? diagnosis.getImageHeight() : 0;

        List<BoundingBoxDto> boxes = coordinateTransformer.toBoundingBoxDtosFromEntities(
                diagnosis.getBoundingBoxes(),
                imgWidth,
                imgHeight
        );

        DiseaseSummaryDto dominantDiseaseDto = resolveDominantDiseaseDto(diagnosis);
        List<DiseaseSummaryDto> coOccurringDiseases = extractCoOccurringDiseases(diagnosis);
        List<TreatmentDto> treatments = resolveRecommendedTreatments(diagnosis, dominantDiseaseDto);

        return new DiagnosisAnalyzeResponse(
                diagnosis.getId(),
                diagnosis.getOriginalImageUrl(),
                imgWidth,
                imgHeight,
                toPlantDto(diagnosis.getPlant()),
                dominantDiseaseDto,
                diagnosis.getDominantConfidence(),
                boxes,
                coOccurringDiseases,
                treatments,
                diagnosis.getInferenceTimeMs() != null ? diagnosis.getInferenceTimeMs() : 0,
                diagnosis.getLatitude(),
                diagnosis.getLongitude(),
                diagnosis.getDeviceInfo(),
                diagnosis.getStatus() != null ? diagnosis.getStatus().name() : "COMPLETED",
                diagnosis.getCreatedAt() != null ? diagnosis.getCreatedAt() : Instant.now()
        );
    }

    @Override
    @Transactional
    public void softDeleteUserDiagnosis(UUID userId, UUID diagnosisId) {
        if (userId == null || diagnosisId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "User ID and Diagnosis ID are required");
        }

        Diagnosis diagnosis = diagnosisRepository.findByIdAndUserIdAndDeletedAtIsNull(diagnosisId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DIAGNOSIS_NOT_FOUND, "Diagnosis session not found or access denied"));

        diagnosis.markDeleted();
        diagnosisRepository.save(diagnosis);
        idempotencyService.evictByDiagnosisId(diagnosisId);
        log.info("Soft-deleted diagnosis session id='{}' for user='{}'", diagnosisId, userId);
    }

    private DiseaseSummaryDto resolveDominantDiseaseDto(Diagnosis diagnosis) {
        if (diagnosis.getDominantDisease() != null) {
            return toDiseaseSummaryDto(diagnosis.getDominantDisease());
        }

        if (diagnosis.getBoundingBoxes() == null || diagnosis.getBoundingBoxes().isEmpty()) {
            return null;
        }

        BoundingBox dominantBox = diagnosis.getBoundingBoxes().stream()
                .max(Comparator.comparingDouble(BoundingBox::getConfidence))
                .orElse(diagnosis.getBoundingBoxes().get(0));

        boolean isHealthy = dominantBox.getDisease() == null ||
                (dominantBox.getLabel() != null && dominantBox.getLabel().toLowerCase().contains("healthy"));

        if (!isHealthy) {
            return null;
        }

        String diseaseCode = null;
        String commonName = null;

        if (dominantBox.getLabel() != null) {
            Optional<RoboflowClassMapper.ClassMetadata> meta = RoboflowClassMapper.fromRawClassName(dominantBox.getLabel());
            if (meta.isPresent()) {
                diseaseCode = meta.get().diseaseCode();
                commonName = meta.get().englishName();
            }
        }

        if (diseaseCode == null) {
            String plantCode = diagnosis.getPlant() != null ? diagnosis.getPlant().getPlantCode() : "CROP";
            diseaseCode = plantCode + "_HEALTHY";
            commonName = diagnosis.getPlant() != null ? diagnosis.getPlant().getCommonName() + " healthy leaf" : "Healthy Foliage";
        }

        return new DiseaseSummaryDto(
                null,
                diseaseCode,
                commonName,
                "Healthy foliage (No pathology detected)",
                Severity.LOW
        );
    }

    private List<DiseaseSummaryDto> extractCoOccurringDiseases(Diagnosis diagnosis) {
        if (diagnosis.getBoundingBoxes() == null || diagnosis.getBoundingBoxes().isEmpty()) {
            return Collections.emptyList();
        }

        UUID dominantDiseaseId = diagnosis.getDominantDisease() != null
                ? diagnosis.getDominantDisease().getId()
                : null;

        return diagnosis.getBoundingBoxes().stream()
                .map(BoundingBox::getDisease)
                .filter(Objects::nonNull)
                .filter(d -> dominantDiseaseId == null || !d.getId().equals(dominantDiseaseId))
                .map(this::toDiseaseSummaryDto)
                .distinct()
                .toList();
    }

    private List<TreatmentDto> resolveRecommendedTreatments(Diagnosis diagnosis, DiseaseSummaryDto dominantDiseaseDto) {
        if (diagnosis.getDominantDisease() != null) {
            return treatmentRecommendationService.getTreatmentsForDisease(diagnosis.getDominantDisease().getId());
        }
        if (dominantDiseaseDto != null && dominantDiseaseDto.diseaseCode() != null && dominantDiseaseDto.diseaseCode().endsWith("_HEALTHY")) {
            String plantCode = diagnosis.getPlant() != null ? diagnosis.getPlant().getPlantCode() : null;
            return treatmentRecommendationService.getHealthyCareTreatments(plantCode);
        }
        return Collections.emptyList();
    }

    private PlantDto toPlantDto(Plant plant) {
        if (plant == null) {
            return null;
        }
        return new PlantDto(
                plant.getId(),
                plant.getPlantCode(),
                plant.getCommonName(),
                plant.getScientificName(),
                plant.getIconUrl(),
                plant.getDescription()
        );
    }

    private DiseaseSummaryDto toDiseaseSummaryDto(Disease disease) {
        if (disease == null) {
            return null;
        }
        return new DiseaseSummaryDto(
                disease.getId(),
                disease.getDiseaseCode(),
                disease.getCommonName(),
                disease.getScientificName(),
                disease.getSeverity()
        );
    }

    private DiagnosisSummaryDto toSummaryDto(Diagnosis diagnosis, int boundingBoxCount) {
        DiseaseSummaryDto dominantDiseaseDto = toDiseaseSummaryDto(diagnosis.getDominantDisease());
        if (dominantDiseaseDto == null && boundingBoxCount > 0) {
            String plantCode = diagnosis.getPlant() != null ? diagnosis.getPlant().getPlantCode() : "CROP";
            String commonName = diagnosis.getPlant() != null
                    ? diagnosis.getPlant().getCommonName() + " healthy leaf"
                    : "Healthy Foliage";
            dominantDiseaseDto = new DiseaseSummaryDto(
                    null,
                    plantCode + "_HEALTHY",
                    commonName,
                    "Healthy foliage (No pathology detected)",
                    Severity.LOW
            );
        }

        return new DiagnosisSummaryDto(
                diagnosis.getId(),
                diagnosis.getOriginalImageUrl(),
                toPlantDto(diagnosis.getPlant()),
                dominantDiseaseDto,
                diagnosis.getDominantConfidence(),
                boundingBoxCount,
                diagnosis.getLatitude(),
                diagnosis.getLongitude(),
                diagnosis.getDeviceInfo(),
                diagnosis.getStatus() != null ? diagnosis.getStatus().name() : "COMPLETED",
                diagnosis.getCreatedAt()
        );
    }
}
