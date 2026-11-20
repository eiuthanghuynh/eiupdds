package com.eiu.pdds.feedback.dto;

public record FeedbackSubmitResult(
        FeedbackResponseDto feedback,
        boolean created
) {
}
