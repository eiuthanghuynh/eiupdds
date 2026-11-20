package com.eiu.pdds.feedback.service;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.UUID;

/**
 * Validates and resolves optional suggested disease references for feedback submission and verification.
 */
@Component
@RequiredArgsConstructor
public class FeedbackDiseaseResolver {

    private final DiseaseRepository diseaseRepository;

    public Disease resolve(Boolean isAccurate, UUID suggestedDiseaseId, String suggestedDiseaseCode) {
        boolean hasId = suggestedDiseaseId != null;
        boolean hasCode = StringUtils.hasText(suggestedDiseaseCode);

        if (Boolean.TRUE.equals(isAccurate) && (hasId || hasCode)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_PAYLOAD,
                    "Suggested disease must not be provided when feedback marks diagnosis as accurate"
            );
        }

        if (!hasId && !hasCode) {
            return null;
        }

        if (hasId) {
            Disease resolvedById = diseaseRepository.findByIdAndDeletedAtIsNull(suggestedDiseaseId)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCode.DISEASE_NOT_FOUND,
                            "Suggested disease not found with ID: " + suggestedDiseaseId
                    ));
            if (hasCode) {
                String normalizedCode = suggestedDiseaseCode.trim().toUpperCase(Locale.ROOT);
                if (!normalizedCode.equalsIgnoreCase(resolvedById.getDiseaseCode())) {
                    throw new BusinessException(
                            ErrorCode.INVALID_REQUEST_PAYLOAD,
                            "suggestedDiseaseId and suggestedDiseaseCode refer to different diseases"
                    );
                }
            }
            return resolvedById;
        }

        String normalizedCode = suggestedDiseaseCode.trim().toUpperCase(Locale.ROOT);
        return diseaseRepository.findByDiseaseCodeAndDeletedAtIsNull(normalizedCode)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.DISEASE_NOT_FOUND,
                        "Suggested disease not found with code: " + normalizedCode
                ));
    }
}
