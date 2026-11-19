package com.eiu.pdds.user.service;

import com.eiu.pdds.common.api.PageResponse;
import com.eiu.pdds.user.dto.UserDetailDto;
import com.eiu.pdds.user.dto.UserFilter;
import com.eiu.pdds.user.dto.UserRoleUpdateRequest;
import com.eiu.pdds.user.dto.UserStatusUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    PageResponse<UserDetailDto> getUsers(UserFilter filter, Pageable pageable);

    UserDetailDto getUserDetail(UUID id);

    UserDetailDto updateUserStatus(UUID targetUserId, UserStatusUpdateRequest request, UUID currentAdminId);

    UserDetailDto updateUserRole(UUID targetUserId, UserRoleUpdateRequest request, UUID currentAdminId);
}
