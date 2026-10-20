package com.eiu.pdds.disease.service;

import com.eiu.pdds.dto.disease.DiseaseDetailDto;
import com.eiu.pdds.dto.disease.DiseaseSummaryDto;

import java.util.List;

public interface DiseaseService {
    List<DiseaseSummaryDto> getAllDiseases();
    DiseaseDetailDto getDiseaseByCode(String diseaseCode);
}
