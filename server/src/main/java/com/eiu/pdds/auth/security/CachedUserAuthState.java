package com.eiu.pdds.auth.security;

import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Objects;
import java.util.UUID;

/**
 * Compact immutable projection of a user's live authorization and account status cached in Redis.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CachedUserAuthState(
        UUID id,
        String email,
        String username,
        Role role,
        boolean active,
        boolean deleted
) {

    @JsonIgnore
    public boolean isUsable() {
        return active && !deleted;
    }

    public static CachedUserAuthState from(User user) {
        Objects.requireNonNull(user, "user must not be null");
        return new CachedUserAuthState(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getRole(),
                Boolean.TRUE.equals(user.getIsActive()),
                user.getDeletedAt() != null
        );
    }
}
