package com.eiu.pdds.user.controller;

import com.eiu.pdds.auth.security.UserPrincipal;
import com.eiu.pdds.common.api.ApiResponse;
import com.eiu.pdds.common.api.PageResponse;
import com.eiu.pdds.user.dto.UserDetailDto;
import com.eiu.pdds.user.dto.UserFilter;
import com.eiu.pdds.user.dto.UserRoleUpdateRequest;
import com.eiu.pdds.user.dto.UserStatusUpdateRequest;
import com.eiu.pdds.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Management (Admin)", description = "Administrative endpoints for managing user accounts, roles, and suspension states")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List users", description = "Retrieves a paginated list of users with dynamic filtering by keyword, role, and active status")
    public ResponseEntity<ApiResponse<PageResponse<UserDetailDto>>> getUsers(
            @ModelAttribute UserFilter filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<UserDetailDto> response = userService.getUsers(filter, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user details", description = "Retrieves full user details including linked providers and total diagnoses count")
    public ResponseEntity<ApiResponse<UserDetailDto>> getUserDetail(@PathVariable UUID id) {
        UserDetailDto detail = userService.getUserDetail(id);
        return ResponseEntity.ok(ApiResponse.success("User details retrieved successfully", detail));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update user active status", description = "Activates or suspends a user. Revokes all active refresh sessions if suspended.")
    public ResponseEntity<ApiResponse<UserDetailDto>> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UserStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentAdmin
    ) {
        UUID currentAdminId = currentAdmin != null ? currentAdmin.id() : null;
        UserDetailDto updated = userService.updateUserStatus(id, request, currentAdminId);
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", updated));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Update user role", description = "Updates a user's role. Prevents administrators from demoting their own account.")
    public ResponseEntity<ApiResponse<UserDetailDto>> updateUserRole(
            @PathVariable UUID id,
            @Valid @RequestBody UserRoleUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentAdmin
    ) {
        UUID currentAdminId = currentAdmin != null ? currentAdmin.id() : null;
        UserDetailDto updated = userService.updateUserRole(id, request, currentAdminId);
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", updated));
    }
}
