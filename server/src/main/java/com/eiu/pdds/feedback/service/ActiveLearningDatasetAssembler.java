package com.eiu.pdds.feedback.service;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.diagnosis.entity.BoundingBox;
import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.diagnosis.repository.BoundingBoxRepository;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.feedback.dto.ActiveLearningExportRow;
import com.eiu.pdds.feedback.entity.DiagnosisFeedback;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Assembles verified active-learning export rows with single-query batch loading of bounding boxes.
 */
@Component
@RequiredArgsConstructor
public class ActiveLearningDatasetAssembler {

    private static final String HEALTHY_CODE = "HEALTHY";
    private static final String UNLABELED_CODE = "UNLABELED";

    private final BoundingBoxRepository boundingBoxRepository;
    private final ObjectMapper objectMapper;

    public List<ActiveLearningExportRow> assemble(List<DiagnosisFeedback> verifiedFeedbacks) {
        if (verifiedFeedbacks == null || verifiedFeedbacks.isEmpty()) {
            return List.of();
        }

        Map<UUID, List<BoundingBox>> boxesByDiagnosisId = loadBoxesByDiagnosisId(verifiedFeedbacks);
        return verifiedFeedbacks.stream()
                .map(feedback -> toExportRow(feedback, boxesByDiagnosisId))
                .toList();
    }

    private Map<UUID, List<BoundingBox>> loadBoxesByDiagnosisId(List<DiagnosisFeedback> feedbacks) {
        Set<UUID> diagnosisIds = feedbacks.stream()
                .map(DiagnosisFeedback::getDiagnosis)
                .filter(Objects::nonNull)
                .map(Diagnosis::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (diagnosisIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return boundingBoxRepository.findByDiagnosisIdIn(diagnosisIds).stream()
                .filter(box -> box.getDiagnosis() != null && box.getDiagnosis().getId() != null)
                .collect(Collectors.groupingBy(box -> box.getDiagnosis().getId()));
    }

    private ActiveLearningExportRow toExportRow(
            DiagnosisFeedback feedback,
            Map<UUID, List<BoundingBox>> boxesByDiagnosisId
    ) {
        Diagnosis diagnosis = feedback.getDiagnosis();
        Disease dominantDisease = diagnosis.getDominantDisease();
        String predictedCode = dominantDisease != null ? dominantDisease.getDiseaseCode() : HEALTHY_CODE;
        String verifiedCode = resolveVerifiedDiseaseCode(feedback, predictedCode);

        List<BoundingBox> boxes = boxesByDiagnosisId.getOrDefault(diagnosis.getId(), List.of());
        return new ActiveLearningExportRow(
                feedback.getId(),
                diagnosis.getId(),
                diagnosis.getOriginalImageUrl(),
                diagnosis.getPlant() != null ? diagnosis.getPlant().getPlantCode() : null,
                predictedCode,
                verifiedCode,
                feedback.getIsAccurate(),
                serializeBoundingBoxes(boxes),
                feedback.getExpertNotes(),
                feedback.getVerifiedAt()
        );
    }

    private String resolveVerifiedDiseaseCode(DiagnosisFeedback feedback, String predictedCode) {
        if (feedback.getSuggestedDisease() != null) {
            return feedback.getSuggestedDisease().getDiseaseCode();
        }
        if (Boolean.TRUE.equals(feedback.getIsAccurate())) {
            return predictedCode;
        }
        return UNLABELED_CODE;
    }

    private String serializeBoundingBoxes(List<BoundingBox> boxes) {
        if (boxes == null || boxes.isEmpty()) {
            return "[]";
        }

        List<ExportBoundingBox> compact = boxes.stream()
                .map(box -> new ExportBoundingBox(
                        box.getLabel(),
                        box.getDisease() != null ? box.getDisease().getDiseaseCode() : null,
                        box.getConfidence(),
                        box.getX(),
                        box.getY(),
                        box.getWidth(),
                        box.getHeight(),
                        box.getX() != null && box.getWidth() != null ? box.getX() - (box.getWidth() / 2.0) : null,
                        box.getY() != null && box.getHeight() != null ? box.getY() - (box.getHeight() / 2.0) : null
                ))
                .toList();

        try {
            return objectMapper.writeValueAsString(compact);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to serialize bounding boxes for export");
        }
    }

    private record ExportBoundingBox(
            String label,
            String diseaseCode,
            Double confidence,
            Double x,
            Double y,
            Double width,
            Double height,
            Double xMin,
            Double yMin
    ) {
    }
}
