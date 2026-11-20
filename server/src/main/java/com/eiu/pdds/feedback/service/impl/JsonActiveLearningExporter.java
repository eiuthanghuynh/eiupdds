package com.eiu.pdds.feedback.service.impl;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.feedback.dto.ActiveLearningExportFormat;
import com.eiu.pdds.feedback.dto.ActiveLearningExportRow;
import com.eiu.pdds.feedback.dto.ExportPayload;
import com.eiu.pdds.feedback.service.ActiveLearningExporter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class JsonActiveLearningExporter implements ActiveLearningExporter {

    private static final String FILENAME = "active-learning-dataset.json";

    private final ObjectMapper objectMapper;

    @Override
    public ActiveLearningExportFormat supportedFormat() {
        return ActiveLearningExportFormat.JSON;
    }

    @Override
    public ExportPayload export(List<ActiveLearningExportRow> rows) {
        List<ActiveLearningExportRow> safeRows = rows != null ? rows : List.of();
        try {
            byte[] payload = objectMapper.writeValueAsBytes(safeRows);
            return new ExportPayload(payload, MediaType.APPLICATION_JSON, FILENAME);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to serialize active-learning dataset as JSON");
        }
    }
}
