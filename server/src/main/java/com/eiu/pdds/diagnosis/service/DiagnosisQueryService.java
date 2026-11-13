package com.eiu.pdds.diagnosis.service;

import com.eiu.pdds.common.api.PageResponse;
import com.eiu.pdds.dto.diagnosis.DiagnosisAnalyzeResponse;
import com.eiu.pdds.dto.diagnosis.DiagnosisHistoryFilter;
import com.eiu.pdds.dto.diagnosis.DiagnosisSummaryDto;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service contract for querying historical diagnoses and session details (SRP & ISP).
 * Completely segregated from AI inference command execution.
 */
public interface DiagnosisQueryService {

    /**
     * Retrieves paginated historical diagnoses for the authenticated user based on optional filters.
     *
     * @param userId   the authenticated user identifier
     * @param filter   filter criteria (crops, diseases, date range, status)
     * @param pageable pagination parameters
     * @return paginated list of lightweight diagnosis summaries
     */
    PageResponse<DiagnosisSummaryDto> getUserHistory(UUID userId, DiagnosisHistoryFilter filter, Pageable pageable);

    /**
     * Retrieves complete diagnosis detail session including dual coordinates and treatment recommendations.
     * Enforces strict user data ownership to prevent IDOR vulnerabilities.
     *
     * @param userId      the authenticated user identifier
     * @param diagnosisId the diagnosis session UUID
     * @return detailed diagnosis response
     */
    DiagnosisAnalyzeResponse getUserDiagnosisDetail(UUID userId, UUID diagnosisId);
}
