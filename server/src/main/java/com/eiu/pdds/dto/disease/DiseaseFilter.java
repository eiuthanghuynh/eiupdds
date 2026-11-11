package com.eiu.pdds.dto.disease;

import com.eiu.pdds.disease.entity.Severity;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Filter parameters for searching and querying disease catalog records.
 */
@Schema(description = "Filter parameters for searching and querying disease records")
public record DiseaseFilter(
        @Schema(description = "Keyword search matching disease code, common name, or scientific name", example = "blight")
        String keyword,

        @Schema(description = "Associated crop code to filter diseases", example = "POTATO")
        String plantCode,

        @Schema(description = "Pathological severity level filter", example = "CRITICAL")
        Severity severity
) {}
