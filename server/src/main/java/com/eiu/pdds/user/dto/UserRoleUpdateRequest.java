package com.eiu.pdds.user.dto;

import com.eiu.pdds.user.entity.Role;
import jakarta.validation.constraints.NotNull;

public record UserRoleUpdateRequest(
        @NotNull(message = "role is required")
        Role role
) {
}
