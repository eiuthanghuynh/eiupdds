package com.eiu.pdds.feedback.service;

import com.eiu.pdds.feedback.dto.ActiveLearningExportFormat;
import com.eiu.pdds.feedback.dto.ActiveLearningExportRow;
import com.eiu.pdds.feedback.dto.ExportPayload;
import com.eiu.pdds.feedback.service.impl.CsvActiveLearningExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CsvActiveLearningExporterTest {

    private CsvActiveLearningExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new CsvActiveLearningExporter();
    }

    @Test
    @DisplayName("Reports CSV as supported format")
    void supportedFormat_ReturnsCsv() {
        assertThat(exporter.supportedFormat()).isEqualTo(ActiveLearningExportFormat.CSV);
    }

    @Test
    @DisplayName("Emits header row and UTF-8 CSV metadata when dataset is empty")
    void export_WhenEmpty_EmitsHeaderRow() {
        ExportPayload payload = exporter.export(List.of());

        assertThat(payload.mediaType()).isEqualTo(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        assertThat(payload.filename()).isEqualTo("active-learning-dataset.csv");

        String csv = new String(payload.content(), StandardCharsets.UTF_8);
        assertThat(csv).startsWith("feedbackId,diagnosisId,originalImageUrl,plantCode,predictedDiseaseCode,verifiedDiseaseCode,isAccurate,boundingBoxesJson,expertNotes,verifiedAt");
        assertThat(csv.trim().lines().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Protects against CSV formula injection for =, +, -, @, tab, and CR prefixes")
    void export_NeutralizesFormulaInjectionPrefixes() {
        UUID feedbackId = UUID.fromString("01900000-0000-7000-8000-000000000001");
        UUID diagnosisId = UUID.fromString("01900000-0000-7000-8000-000000000002");
        Instant verifiedAt = Instant.parse("2026-11-20T10:15:30Z");

        List<ActiveLearningExportRow> rows = List.of(
                row(feedbackId, diagnosisId, "=CMD|' /C calc'!A0", verifiedAt),
                row(feedbackId, diagnosisId, "+SUM(A1:A2)", verifiedAt),
                row(feedbackId, diagnosisId, "-2+3", verifiedAt),
                row(feedbackId, diagnosisId, "@SUM(1+1)", verifiedAt),
                row(feedbackId, diagnosisId, "\t=1+1", verifiedAt),
                row(feedbackId, diagnosisId, "\r=1+1", verifiedAt)
        );

        String csv = new String(exporter.export(rows).content(), StandardCharsets.UTF_8);

        assertThat(csv).contains("'=CMD|' /C calc'!A0");
        assertThat(csv).contains("'+SUM(A1:A2)");
        assertThat(csv).contains("'-2+3");
        assertThat(csv).contains("'@SUM(1+1)");
        assertThat(csv).contains("'\t=1+1");
        assertThat(csv).contains("\"'\r=1+1\"");
    }

    @Test
    @DisplayName("Escapes commas, quotes, and newlines per RFC-4180")
    void export_EscapesCommasQuotesAndNewlines() {
        UUID feedbackId = UUID.fromString("01900000-0000-7000-8000-000000000010");
        UUID diagnosisId = UUID.fromString("01900000-0000-7000-8000-000000000020");
        Instant verifiedAt = Instant.parse("2026-11-20T12:00:00Z");

        ActiveLearningExportRow row = new ActiveLearningExportRow(
                feedbackId,
                diagnosisId,
                "https://cdn.eiu.edu.vn/leaf.jpg",
                "POTATO",
                "POTATO_LATE_BLIGHT",
                "POTATO_EARLY_BLIGHT",
                false,
                "[{\"label\":\"Potato leaf\",\"confidence\":0.92}]",
                "Line 1, with \"quotes\"\nLine 2",
                verifiedAt
        );

        String csv = new String(exporter.export(List.of(row)).content(), StandardCharsets.UTF_8);

        assertThat(csv).contains("\"[{\"\"label\"\":\"\"Potato leaf\"\",\"\"confidence\"\":0.92}]\"");
        assertThat(csv).contains("\"Line 1, with \"\"quotes\"\"\nLine 2\"");
        assertThat(csv).contains("2026-11-20T12:00:00Z");
    }

    private ActiveLearningExportRow row(UUID feedbackId, UUID diagnosisId, String notes, Instant verifiedAt) {
        return new ActiveLearningExportRow(
                feedbackId,
                diagnosisId,
                "https://cdn.eiu.edu.vn/leaf.jpg",
                "TOMATO",
                "TOMATO_EARLY_BLIGHT",
                "TOMATO_EARLY_BLIGHT",
                true,
                "[]",
                notes,
                verifiedAt
        );
    }
}
