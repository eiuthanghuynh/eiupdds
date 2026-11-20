package com.eiu.pdds.feedback.service;

import com.eiu.pdds.diagnosis.entity.Diagnosis;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.feedback.dto.FeedbackResponseDto;
import com.eiu.pdds.feedback.entity.DiagnosisFeedback;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.user.entity.User;
import org.springframework.stereotype.Component;

/**
 * Null-safe mapper transforming DiagnosisFeedback entities into API response DTOs.
 */
@Component
public class FeedbackMapper {

    public FeedbackResponseDto toDto(DiagnosisFeedback feedback) {
        if (feedback == null) {
            return null;
        }

        Diagnosis diagnosis = feedback.getDiagnosis();
        Plant plant = diagnosis != null ? diagnosis.getPlant() : null;
        Disease predictedDisease = diagnosis != null ? diagnosis.getDominantDisease() : null;
        User submitter = feedback.getUser();
        Disease suggestedDisease = feedback.getSuggestedDisease();
        User verifier = feedback.getVerifiedByUser();

        return new FeedbackResponseDto(
                feedback.getId(),
                diagnosis != null ? diagnosis.getId() : null,
                diagnosis != null ? diagnosis.getOriginalImageUrl() : null,
                plant != null ? plant.getPlantCode() : null,
                plant != null ? plant.getCommonName() : null,
                predictedDisease != null ? predictedDisease.getDiseaseCode() : null,
                predictedDisease != null ? predictedDisease.getCommonName() : null,
                diagnosis != null ? diagnosis.getDominantConfidence() : null,
                submitter != null ? submitter.getId() : null,
                submitter != null ? submitter.getUsername() : null,
                feedback.getIsAccurate(),
                suggestedDisease != null ? suggestedDisease.getId() : null,
                suggestedDisease != null ? suggestedDisease.getDiseaseCode() : null,
                suggestedDisease != null ? suggestedDisease.getCommonName() : null,
                feedback.getUserComment(),
                feedback.getExpertVerified(),
                verifier != null ? verifier.getId() : null,
                verifier != null ? verifier.getUsername() : null,
                feedback.getVerifiedAt(),
                feedback.getExpertNotes(),
                feedback.getCreatedAt(),
                feedback.getUpdatedAt()
        );
    }
}
