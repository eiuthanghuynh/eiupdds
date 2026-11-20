package com.eiu.pdds.feedback.dto;

import org.springframework.http.MediaType;

public record ExportPayload(
        byte[] content,
        MediaType mediaType,
        String filename
) {
}
