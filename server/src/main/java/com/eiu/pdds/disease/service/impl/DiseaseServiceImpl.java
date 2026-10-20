package com.eiu.pdds.disease.service.impl;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.disease.service.DiseaseService;
import com.eiu.pdds.dto.disease.DiseaseDetailDto;
import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import com.eiu.pdds.dto.treatment.TreatmentDto;
import com.eiu.pdds.treatment.entity.Treatment;
import com.eiu.pdds.treatment.repository.TreatmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DiseaseServiceImpl implements DiseaseService {

    private final DiseaseRepository diseaseRepository;
    private final TreatmentRepository treatmentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DiseaseSummaryDto> getAllDiseases() {
        return diseaseRepository.findByDeletedAtIsNull().stream()
                .map(this::toSummaryDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DiseaseDetailDto getDiseaseByCode(String diseaseCode) {
        Disease disease = diseaseRepository.findByDiseaseCode(diseaseCode.toUpperCase())
                .filter(d -> !d.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Disease not found with code: " + diseaseCode));

        List<TreatmentDto> treatments = treatmentRepository.findByDiseaseIdAndDeletedAtIsNull(disease.getId()).stream()
                .map(this::toTreatmentDto)
                .toList();

        return new DiseaseDetailDto(
                disease.getId(),
                disease.getDiseaseCode(),
                disease.getCommonName(),
                disease.getScientificName(),
                disease.getSeverity(),
                disease.getSymptoms(),
                disease.getDescription(),
                treatments
        );
    }

    private DiseaseSummaryDto toSummaryDto(Disease disease) {
        return new DiseaseSummaryDto(
                disease.getId(),
                disease.getDiseaseCode(),
                disease.getCommonName(),
                disease.getScientificName(),
                disease.getSeverity()
        );
    }

    private TreatmentDto toTreatmentDto(Treatment treatment) {
        return new TreatmentDto(
                treatment.getId(),
                treatment.getTreatmentType(),
                treatment.getTitle(),
                treatment.getInstructions(),
                treatment.getActiveIngredients(),
                treatment.getDosageRecommendation(),
                treatment.getSafetyIntervalDays(),
                treatment.getWarning()
        );
    }
}
