package com.eiu.pdds.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request payload for expert or admin verification of submitted diagnosis feedback")
public record FeedbackVerifyRequest(
        @Schema(description = "Optional override for whether the AI prediction was accurate")
        Boolean isAccurate,

        @Schema(description = "Optional UUID of the verified disease when isAccurate is false")
        UUID suggestedDiseaseId,

        @Schema(description = "Optional code of the verified disease when isAccurate is false", example = "POTATO_EARLY_BLIGHT")
        String suggestedDiseaseCode,

        @Size(max = 2000, message = "expertNotes must not exceed 2000 characters")
        @Schema(description = "Agronomist notes documenting the verification decision")
        String expertNotes
) {
}
