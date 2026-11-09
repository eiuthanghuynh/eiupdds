package com.eiu.pdds.disease.service.impl;

import com.eiu.pdds.disease.service.DiseaseQueryService;
import com.eiu.pdds.disease.service.DiseaseService;
import com.eiu.pdds.dto.disease.DiseaseDetailDto;
import com.eiu.pdds.dto.disease.DiseaseSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Backward-compatible facade implementation of DiseaseService delegating to DiseaseQueryService.
 */
@Service
@RequiredArgsConstructor
public class DiseaseServiceImpl implements DiseaseService {

    private final DiseaseQueryService diseaseQueryService;

    @Override
    @Transactional(readOnly = true)
    public List<DiseaseSummaryDto> getAllDiseases() {
        return diseaseQueryService.getDiseases(null, null, true).content();
    }

    @Override
    @Transactional(readOnly = true)
    public DiseaseDetailDto getDiseaseByCode(String diseaseCode) {
        return diseaseQueryService.getDiseaseByIdOrCode(diseaseCode);
    }
}
