package com.eiu.pdds.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INTERNAL_SERVER_ERROR("SERVER_001", "Internal server error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST_PAYLOAD("REQ_001", "Invalid request payload or missing parameters", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE("FILE_001", "Uploaded file exceeds maximum allowed limit (15MB)", HttpStatus.PAYLOAD_TOO_LARGE),
    INVALID_FILE_TYPE("FILE_002", "Only JPEG, PNG, and WebP image formats are supported", HttpStatus.BAD_REQUEST),
    ROBOFLOW_API_ERROR("AI_001", "Roboflow inference API returned an error or timed out", HttpStatus.BAD_GATEWAY),
    DISEASE_NOT_FOUND("DISEASE_001", "Disease record not found", HttpStatus.NOT_FOUND),
    PLANT_NOT_FOUND("PLANT_001", "Plant crop record not found", HttpStatus.NOT_FOUND),
    RESOURCE_NOT_FOUND("RES_001", "Requested resource not found", HttpStatus.NOT_FOUND),
    DIAGNOSIS_NOT_FOUND("DIAGNOSIS_001", "Diagnosis session not found", HttpStatus.NOT_FOUND);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
