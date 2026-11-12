package com.eiu.pdds.user.service;

import com.eiu.pdds.auth.service.RefreshTokenStore;
import com.eiu.pdds.common.api.PageResponse;
import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.user.dto.UserDetailDto;
import com.eiu.pdds.user.dto.UserFilter;
import com.eiu.pdds.user.dto.UserRoleUpdateRequest;
import com.eiu.pdds.user.dto.UserStatusUpdateRequest;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.entity.UserIdentity;
import com.eiu.pdds.user.repository.UserIdentityRepository;
import com.eiu.pdds.user.repository.UserRepository;
import com.eiu.pdds.user.repository.UserSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final RefreshTokenStore refreshTokenStore;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDetailDto> getUsers(UserFilter filter, Pageable pageable) {
        Page<User> userPage = userRepository.findAll(UserSpecifications.withFilter(filter), pageable);
        Page<UserDetailDto> dtoPage = userPage.map(this::toUserDetailDto);
        return PageResponse.of(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailDto getUserDetail(UUID id) {
        User user = userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return toUserDetailDto(user);
    }

    @Override
    @Transactional
    public UserDetailDto updateUserStatus(UUID targetUserId, UserStatusUpdateRequest request, UUID currentAdminId) {
        if (targetUserId != null && targetUserId.equals(currentAdminId)) {
            throw new BusinessException(ErrorCode.CANNOT_MODIFY_OWN_ACCOUNT, "Administrators cannot modify their own active status");
        }

        User targetUser = userRepository.findById(targetUserId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        targetUser.setIsActive(request.isActive());
        User savedUser = userRepository.save(targetUser);

        if (Boolean.FALSE.equals(request.isActive())) {
            log.info("Revoking active sessions for suspended user: id={}", targetUserId);
            refreshTokenStore.revokeAllForUser(targetUserId);
        }

        log.info("Updated account status for user: id={}, isActive={}", targetUserId, request.isActive());
        return toUserDetailDto(savedUser);
    }

    @Override
    @Transactional
    public UserDetailDto updateUserRole(UUID targetUserId, UserRoleUpdateRequest request, UUID currentAdminId) {
        if (targetUserId != null && targetUserId.equals(currentAdminId)) {
            throw new BusinessException(ErrorCode.CANNOT_MODIFY_OWN_ACCOUNT, "Administrators cannot demote their own admin role");
        }

        User targetUser = userRepository.findById(targetUserId)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        targetUser.setRole(request.role());
        User savedUser = userRepository.save(targetUser);

        log.info("Updated role for user: id={}, role={}", targetUserId, request.role());
        return toUserDetailDto(savedUser);
    }

    private UserDetailDto toUserDetailDto(User user) {
        List<UserIdentity> identities = userIdentityRepository.findByUserId(user.getId());
        long diagnosesCount = diagnosisRepository.countByUserIdAndDeletedAtIsNull(user.getId());
        return UserDetailDto.from(user, identities, diagnosesCount);
    }
}
