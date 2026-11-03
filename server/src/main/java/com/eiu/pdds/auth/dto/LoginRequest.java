package com.eiu.pdds.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "User credentials login payload")
public record LoginRequest(
        @Schema(description = "Registered email or username", example = "john.doe@example.com")
        @NotBlank(message = "Username or email is required")
        String emailOrUsername,

        @Schema(description = "Plaintext password", example = "SecretPass123!")
        @NotBlank(message = "Password is required")
        String password,

        @Schema(description = "Client device identifier", example = "iPhone 15 Pro / iOS 17.5")
        String deviceInfo
) {}
