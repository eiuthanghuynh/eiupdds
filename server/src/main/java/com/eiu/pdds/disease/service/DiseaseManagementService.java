package com.eiu.pdds.disease.service;

import com.eiu.pdds.dto.disease.DiseaseCreateRequest;
import com.eiu.pdds.dto.disease.DiseaseDetailDto;
import com.eiu.pdds.dto.disease.DiseaseUpdateRequest;

import java.util.UUID;

/**
 * Service interface for managing plant disease catalog entries (CQRS Command model).
 */
public interface DiseaseManagementService {

    /**
     * Creates a new plant disease catalog record with optional host plant associations.
     *
     * @param request creation payload
     * @return newly created disease detail record
     */
    DiseaseDetailDto createDisease(DiseaseCreateRequest request);

    /**
     * Updates an existing active plant disease catalog entry.
     *
     * @param id      disease UUID
     * @param request update payload
     * @return updated disease detail record
     */
    DiseaseDetailDto updateDisease(UUID id, DiseaseUpdateRequest request);

    /**
     * Soft-deletes a plant disease catalog entry, preserving historical records and AI telemetry.
     *
     * @param id disease UUID
     */
    void deleteDisease(UUID id);
}
