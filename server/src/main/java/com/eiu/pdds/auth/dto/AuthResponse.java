package com.eiu.pdds.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response containing JWT tokens and user profile")
public record AuthResponse(
        @Schema(description = "Stateless JWT Access Token (15 min lifespan)", example = "eyJhbGciOiJIUzI1NiIsIn...")
        String accessToken,

        @Schema(description = "Opaque Refresh Token (7 days lifespan)", example = "550e8400-e29b-41d4-a716-446655440000-...")
        String refreshToken,

        @Schema(description = "Token authorization type", example = "Bearer")
        String tokenType,

        @Schema(description = "Access token validity in milliseconds", example = "900000")
        long expiresIn,

        @Schema(description = "Authenticated user profile")
        UserSummaryDto user
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresIn, UserSummaryDto user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
