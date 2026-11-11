package com.eiu.pdds.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Google OAuth2 ID Token exchange payload")
public record GoogleLoginRequest(
        @Schema(description = "Raw Google ID Token JWT from client SDK", example = "eyJhbGciOiJSUzI1NiIs...")
        @NotBlank(message = "Google ID token is required")
        String idToken,

        @Schema(description = "Client device identifier", example = "Android Galaxy S24 / Android 14")
        String deviceInfo
) {}
