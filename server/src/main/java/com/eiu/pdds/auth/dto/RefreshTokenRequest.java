package com.eiu.pdds.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh Token exchange payload")
public record RefreshTokenRequest(
        @Schema(description = "Valid opaque Refresh Token", example = "550e8400-e29b-41d4-a716-446655440000-...")
        @NotBlank(message = "Refresh token is required")
        String refreshToken,

        @Schema(description = "Client device identifier", example = "iPhone 15 Pro / iOS 17.5")
        String deviceInfo
) {
    public RefreshTokenRequest(String refreshToken) {
        this(refreshToken, null);
    }
}
