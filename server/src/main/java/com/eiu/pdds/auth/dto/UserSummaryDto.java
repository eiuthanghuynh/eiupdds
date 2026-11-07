package com.eiu.pdds.auth.dto;

import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Compact user summary DTO")
public record UserSummaryDto(
        @Schema(description = "User unique UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID id,

        @Schema(description = "Username", example = "farmer_john")
        String username,

        @Schema(description = "Email address", example = "john.doe@example.com")
        String email,

        @Schema(description = "Full name", example = "John Doe")
        String fullName,

        @Schema(description = "Assigned security role", example = "ROLE_USER")
        Role role,

        @Schema(description = "Avatar photo URL", example = "https://lh3.googleusercontent.com/a/...")
        String avatarUrl
) {
    public static UserSummaryDto fromEntity(User user) {
        return new UserSummaryDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getAvatarUrl()
        );
    }
}
