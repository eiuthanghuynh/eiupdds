package com.eiu.pdds.feedback.service.impl;

import com.eiu.pdds.feedback.dto.ActiveLearningExportFormat;
import com.eiu.pdds.feedback.dto.ActiveLearningExportRow;
import com.eiu.pdds.feedback.dto.ExportPayload;
import com.eiu.pdds.feedback.service.ActiveLearningExporter;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * RFC-4180 compliant CSV exporter with spreadsheet formula-injection neutralization.
 */
@Component
public class CsvActiveLearningExporter implements ActiveLearningExporter {

    private static final String FILENAME = "active-learning-dataset.csv";
    private static final MediaType CSV_UTF8 = MediaType.parseMediaType("text/csv; charset=UTF-8");
    private static final String HEADER =
            "feedbackId,diagnosisId,originalImageUrl,plantCode,predictedDiseaseCode,verifiedDiseaseCode,isAccurate,boundingBoxesJson,expertNotes,verifiedAt";
    private static final String CRLF = "\r\n";

    @Override
    public ActiveLearningExportFormat supportedFormat() {
        return ActiveLearningExportFormat.CSV;
    }

    @Override
    public ExportPayload export(List<ActiveLearningExportRow> rows) {
        StringBuilder builder = new StringBuilder();
        builder.append(HEADER).append(CRLF);

        if (rows != null) {
            for (ActiveLearningExportRow row : rows) {
                appendRow(builder, row);
            }
        }

        return new ExportPayload(builder.toString().getBytes(StandardCharsets.UTF_8), CSV_UTF8, FILENAME);
    }

    private void appendRow(StringBuilder builder, ActiveLearningExportRow row) {
        builder.append(formatCell(row.feedbackId())).append(',')
                .append(formatCell(row.diagnosisId())).append(',')
                .append(formatCell(row.originalImageUrl())).append(',')
                .append(formatCell(row.plantCode())).append(',')
                .append(formatCell(row.predictedDiseaseCode())).append(',')
                .append(formatCell(row.verifiedDiseaseCode())).append(',')
                .append(formatCell(row.isAccurate())).append(',')
                .append(formatCell(row.boundingBoxesJson())).append(',')
                .append(formatCell(row.expertNotes())).append(',')
                .append(formatCell(row.verifiedAt()))
                .append(CRLF);
    }

    private String formatCell(Object rawValue) {
        if (rawValue == null) {
            return "";
        }

        String text = rawValue.toString();
        if (!text.isEmpty() && isFormulaTrigger(text.charAt(0))) {
            text = "'" + text;
        }

        boolean needsQuoting = text.indexOf(',') >= 0
                || text.indexOf('"') >= 0
                || text.indexOf('\n') >= 0
                || text.indexOf('\r') >= 0;

        if (!needsQuoting) {
            return text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private boolean isFormulaTrigger(char firstChar) {
        return firstChar == '='
                || firstChar == '+'
                || firstChar == '-'
                || firstChar == '@'
                || firstChar == '\t'
                || firstChar == '\r';
    }
}
