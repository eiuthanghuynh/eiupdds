package com.eiu.pdds.user.service;

import com.eiu.pdds.auth.security.UserAuthCache;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.stream.Collectors;

import com.eiu.pdds.auth.service.RefreshTokenStore;
import com.eiu.pdds.common.api.PageResponse;
import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.user.dto.UserDetailDto;
import com.eiu.pdds.user.dto.UserFilter;
import com.eiu.pdds.user.dto.UserRoleUpdateRequest;
import com.eiu.pdds.user.dto.UserStatusUpdateRequest;
import com.eiu.pdds.user.entity.Role;
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
    private final UserAuthCache userAuthCache;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDetailDto> getUsers(UserFilter filter, Pageable pageable) {
        Page<User> userPage = userRepository.findAll(UserSpecifications.withFilter(filter), pageable);
        if (userPage.isEmpty()) {
            return PageResponse.of(Page.empty(pageable));
        }

        List<UUID> userIds = userPage.getContent().stream()
                .map(User::getId)
                .toList();

        Map<UUID, List<UserIdentity>> identitiesByUserId = userIdentityRepository.findByUserIdIn(userIds).stream()
                .filter(identity -> identity.getUser() != null && identity.getUser().getId() != null)
                .collect(Collectors.groupingBy(identity -> identity.getUser().getId()));

        Map<UUID, Long> diagnosisCountsByUserId = diagnosisRepository.countByUserIds(userIds).stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> ((Number) row[1]).longValue(),
                        Long::sum
                ));

        Page<UserDetailDto> dtoPage = userPage.map(user -> UserDetailDto.from(
                user,
                identitiesByUserId.getOrDefault(user.getId(), List.of()),
                diagnosisCountsByUserId.getOrDefault(user.getId(), 0L)
        ));
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

        evictUserAuthCacheSafely(targetUserId);
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

        Role previousRole = targetUser.getRole();
        targetUser.setRole(request.role());
        User savedUser = userRepository.save(targetUser);

        if (previousRole != request.role()) {
            log.info("Revoking active refresh sessions after role change for user: id={}, from={}, to={}",
                    targetUserId, previousRole, request.role());
            refreshTokenStore.revokeAllForUser(targetUserId);
        }

        evictUserAuthCacheSafely(targetUserId);
        log.info("Updated role for user: id={}, role={}", targetUserId, request.role());
        return toUserDetailDto(savedUser);
    }

    private void evictUserAuthCacheSafely(UUID userId) {
        try {
            userAuthCache.evict(userId);
        } catch (Exception e) {
            log.warn("Immediate user auth cache eviction failed for userId={}: {}", userId, e.getMessage());
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        userAuthCache.evict(userId);
                    } catch (Exception e) {
                        log.warn("Post-commit user auth cache eviction failed for userId={}: {}", userId, e.getMessage());
                    }
                }
            });
        }
    }

    private UserDetailDto toUserDetailDto(User user) {
        List<UserIdentity> identities = userIdentityRepository.findByUserId(user.getId());
        long diagnosesCount = diagnosisRepository.countByUserIdAndDeletedAtIsNull(user.getId());
        return UserDetailDto.from(user, identities, diagnosesCount);
    }
}
