package com.eiu.pdds.auth.security;

import com.eiu.pdds.user.entity.Role;

import java.security.Principal;
import java.util.UUID;

/**
 * Authenticated principal placed in Spring SecurityContextHolder.
 */
public record UserPrincipal(
        UUID id,
        String email,
        String username,
        Role role
) implements Principal {

    @Override
    public String getName() {
        return id.toString();
    }
}
