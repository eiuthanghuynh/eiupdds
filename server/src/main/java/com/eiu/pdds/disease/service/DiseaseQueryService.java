package com.eiu.pdds.disease.service;

import com.eiu.pdds.common.api.PageResponse;
import com.eiu.pdds.dto.disease.DiseaseDetailDto;
import com.eiu.pdds.dto.disease.DiseaseFilter;
import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for querying plant disease catalog records (CQRS Query model).
 */
public interface DiseaseQueryService {

    /**
     * Retrieves a paginated or unpaged list of diseases matching optional filter criteria.
     *
     * @param filter   filter criteria (keyword, plantCode, severity)
     * @param pageable pagination and sorting parameters
     * @param unpaged  whether to return all matching diseases unpaged
     * @return paginated response containing disease summaries
     */
    PageResponse<DiseaseSummaryDto> getDiseases(DiseaseFilter filter, Pageable pageable, boolean unpaged);

    /**
     * Retrieves detailed disease information including mitigation treatments by either UUID or disease code.
     *
     * @param idOrCode UUID string or standardized disease code
     * @return comprehensive disease detail DTO
     */
    DiseaseDetailDto getDiseaseByIdOrCode(String idOrCode);
}
