package com.eiu.pdds.treatment.service;

import com.eiu.pdds.dto.treatment.TreatmentCreateRequest;
import com.eiu.pdds.dto.treatment.TreatmentDto;
import com.eiu.pdds.dto.treatment.TreatmentUpdateRequest;
import com.eiu.pdds.treatment.entity.TreatmentType;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing agricultural treatment and mitigation protocols (SRP & DIP).
 */
public interface TreatmentManagementService {

    /**
     * Retrieves active treatments for a specific disease, optionally filtered by treatment type and crop plant.
     *
     * @param diseaseId     UUID of the disease
     * @param treatmentType optional treatment category filter (BIOLOGICAL, CHEMICAL, PREVENTIVE, CULTURAL)
     * @param plantId       optional plant crop UUID filter
     * @return list of matching treatment protocols
     */
    List<TreatmentDto> getTreatmentsByDiseaseId(UUID diseaseId, TreatmentType treatmentType, UUID plantId);

    /**
     * Creates a new treatment protocol linked to a disease and optionally a specific plant crop.
     *
     * @param diseaseId disease UUID
     * @param request   treatment creation payload
     * @return newly created treatment protocol
     */
    TreatmentDto createTreatment(UUID diseaseId, TreatmentCreateRequest request);

    /**
     * Updates an existing active treatment protocol.
     *
     * @param treatmentId treatment UUID
     * @param request     treatment update payload
     * @return updated treatment protocol
     */
    TreatmentDto updateTreatment(UUID treatmentId, TreatmentUpdateRequest request);

    /**
     * Soft-deletes a treatment protocol, preserving historical audit logs.
     *
     * @param treatmentId treatment UUID
     */
    void deleteTreatment(UUID treatmentId);
}
