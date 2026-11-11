package com.eiu.pdds.treatment.service;

import com.eiu.pdds.dto.treatment.TreatmentDto;

import java.util.List;
import java.util.UUID;

/**
 * Service interface managing therapeutic and preventive agricultural treatments (SRP & OCP).
 */
public interface TreatmentRecommendationService {

    /**
     * Retrieves curated treatment protocols for a specific plant pathology.
     */
    List<TreatmentDto> getTreatmentsForDisease(UUID diseaseId);

    /**
     * Retrieves preventive monitoring and routine cultural care guidelines for healthy foliage.
     */
    List<TreatmentDto> getHealthyCareTreatments(String plantCode);
}
