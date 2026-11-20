package com.eiu.pdds.feedback.service;

import com.eiu.pdds.feedback.dto.ActiveLearningExportFormat;
import com.eiu.pdds.feedback.dto.ActiveLearningExportRow;
import com.eiu.pdds.feedback.dto.ExportPayload;

import java.util.List;

/**
 * Strategy contract for serializing curated active-learning dataset rows into downloadable artifacts.
 */
public interface ActiveLearningExporter {

    ActiveLearningExportFormat supportedFormat();

    ExportPayload export(List<ActiveLearningExportRow> rows);
}
