package com.eiu.pdds.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request payload for submitting or updating farmer feedback on a diagnosis session")
public record FeedbackSubmitRequest(
        @NotNull(message = "isAccurate is required")
        @Schema(description = "Whether the AI prediction matched actual leaf condition", example = "false")
        Boolean isAccurate,

        @Schema(description = "Optional UUID of the corrected disease when isAccurate is false")
        UUID suggestedDiseaseId,

        @Schema(description = "Optional code of the corrected disease when isAccurate is false", example = "POTATO_EARLY_BLIGHT")
        String suggestedDiseaseCode,

        @Size(max = 2000, message = "userComment must not exceed 2000 characters")
        @Schema(description = "Optional field observation notes from the farmer")
        String userComment
) {
}
