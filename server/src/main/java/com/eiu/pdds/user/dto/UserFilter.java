package com.eiu.pdds.user.dto;

import com.eiu.pdds.user.entity.Role;

public record UserFilter(
        String keyword,
        Role role,
        Boolean isActive
) {
}
