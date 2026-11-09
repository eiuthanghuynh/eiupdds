package com.eiu.pdds.dto.diagnosis;

import com.eiu.pdds.diagnosis.entity.DiagnosisStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

public record DiagnosisHistoryFilter(
        String plantCode,
        String diseaseCode,
        DiagnosisStatus status,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant fromDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant toDate
) {
}
