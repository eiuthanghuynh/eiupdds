package com.eiu.pdds.disease.service.impl;

import com.eiu.pdds.common.api.PageResponse;
import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.disease.repository.DiseaseSpecifications;
import com.eiu.pdds.disease.service.DiseaseQueryService;
import com.eiu.pdds.dto.disease.DiseaseDetailDto;
import com.eiu.pdds.dto.disease.DiseaseFilter;
import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import com.eiu.pdds.dto.treatment.TreatmentDto;
import com.eiu.pdds.treatment.entity.Treatment;
import com.eiu.pdds.treatment.repository.TreatmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Production implementation of DiseaseQueryService (CQRS-lite Query Model, SRP).
 * Specializes in efficient, read-only queries with multi-criteria dynamic filtering and pagination.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiseaseQueryServiceImpl implements DiseaseQueryService {

    private final DiseaseRepository diseaseRepository;
    private final TreatmentRepository treatmentRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DiseaseSummaryDto> getDiseases(DiseaseFilter filter, Pageable pageable, boolean unpaged) {
        Specification<Disease> spec = DiseaseSpecifications.withFilter(filter);

        Pageable effectivePageable;
        if (unpaged) {
            effectivePageable = (pageable != null && pageable.getSort().isSorted())
                    ? Pageable.unpaged(pageable.getSort())
                    : Pageable.unpaged();
        } else {
            effectivePageable = pageable != null ? pageable : PageRequest.of(0, 20);
        }

        Page<Disease> page = diseaseRepository.findAll(spec, effectivePageable);
        return PageResponse.of(page.map(this::toSummaryDto));
    }

    @Override
    @Transactional(readOnly = true)
    public DiseaseDetailDto getDiseaseByIdOrCode(String idOrCode) {
        if (idOrCode == null || idOrCode.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Disease ID or code is required");
        }

        String trimmed = idOrCode.trim();
        Optional<UUID> uuidOpt = tryParseUuid(trimmed);
        Disease disease = null;

        if (uuidOpt.isPresent()) {
            disease = diseaseRepository.findById(uuidOpt.get())
                    .filter(d -> !d.isDeleted())
                    .orElse(null);
        }

        if (disease == null) {
            disease = diseaseRepository.findByDiseaseCode(trimmed.toUpperCase())
                    .filter(d -> !d.isDeleted())
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                            "Disease not found with ID or code: " + idOrCode));
        }

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

    private Optional<UUID> tryParseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
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
