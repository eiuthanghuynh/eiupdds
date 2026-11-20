package com.eiu.pdds.feedback.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

public record FeedbackQueueFilter(
        Boolean expertVerified,
        Boolean isAccurate,
        String plantCode,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant fromDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant toDate
) {
}
