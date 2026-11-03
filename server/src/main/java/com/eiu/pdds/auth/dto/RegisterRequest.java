package com.eiu.pdds.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "User registration payload")
public record RegisterRequest(
        @Schema(description = "Unique username", example = "farmer_john")
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        String username,

        @Schema(description = "User email address", example = "john.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email format")
        String email,

        @Schema(description = "Password (min 6 characters)", example = "SecretPass123!")
        @NotBlank(message = "Password is required")
        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password,

        @Schema(description = "Full display name", example = "John Doe")
        String fullName,

        @Schema(description = "Contact phone number", example = "+84912345678")
        String phoneNumber
) {}
