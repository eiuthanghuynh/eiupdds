package com.eiu.pdds.user.dto;

import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.entity.UserIdentity;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record UserDetailDto(
        UUID id,
        String username,
        String email,
        String fullName,
        String avatarUrl,
        String phoneNumber,
        Role role,
        Boolean isActive,
        List<String> linkedProviders,
        long totalDiagnosesCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static UserDetailDto from(User user, List<UserIdentity> identities, long totalDiagnosesCount) {
        List<String> providers = identities == null ? List.of() :
                identities.stream()
                        .map(identity -> identity.getProvider() != null ? identity.getProvider().name() : null)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

        return new UserDetailDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getIsActive(),
                providers,
                totalDiagnosesCount,
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
