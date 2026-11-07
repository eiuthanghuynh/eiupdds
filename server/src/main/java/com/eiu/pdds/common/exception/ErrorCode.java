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
    DIAGNOSIS_NOT_FOUND("DIAGNOSIS_001", "Diagnosis session not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED("AUTH_001", "Full authentication is required to access this resource", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("AUTH_002", "Invalid username/email or password", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AUTH_003", "Access denied: insufficient permissions", HttpStatus.FORBIDDEN),
    USER_ALREADY_EXISTS("AUTH_004", "Username or email is already registered", HttpStatus.CONFLICT),
    INVALID_REFRESH_TOKEN("AUTH_005", "Refresh token is invalid, expired, or has been revoked", HttpStatus.UNAUTHORIZED),
    TOKEN_REUSE_DETECTED("AUTH_006", "Security alert: token reuse detected, all active sessions terminated", HttpStatus.UNAUTHORIZED),
    GOOGLE_AUTH_FAILED("AUTH_007", "Google ID token verification failed", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("AUTH_008", "User account not found", HttpStatus.NOT_FOUND),
    USER_INACTIVE("AUTH_009", "User account is suspended or inactive", HttpStatus.FORBIDDEN);

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
