package com.eiu.pdds.user.dto;

import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
        @NotNull(message = "isActive is required")
        Boolean isActive,
        String reason
) {
}
