package com.eiu.pdds.feedback.dto;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;

import java.util.Locale;

public enum ActiveLearningExportFormat {
    JSON,
    CSV;

    public static ActiveLearningExportFormat fromString(String raw) {
        if (raw == null || raw.isBlank()) {
            return JSON;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (ActiveLearningExportFormat format : values()) {
            if (format.name().equals(normalized)) {
                return format;
            }
        }
        throw new BusinessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "Unsupported export format: " + raw);
    }
}
