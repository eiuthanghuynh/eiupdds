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
import com.eiu.pdds.user.entity.AuthProvider;
import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.entity.UserIdentity;
import com.eiu.pdds.user.repository.UserIdentityRepository;
import com.eiu.pdds.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit Test suite for {@link UserService} validating User Management, Pagination,
 * Administrative status/role mutation, Self-Lockout Guard, and Self-Demotion Guard.
 *
 * <p>Phase: TDD RED Phase (Task 4.1).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @Mock
    private DiagnosisRepository diagnosisRepository;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID adminId;
    private UUID targetUserId;
    private User adminUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();

        adminUser = User.builder()
                .username("system_admin")
                .email("admin@eiu.edu.vn")
                .fullName("System Administrator")
                .role(Role.ROLE_ADMIN)
                .isActive(true)
                .build();
        adminUser.setId(adminId);

        targetUser = User.builder()
                .username("farmer_john")
                .email("john@farm.vn")
                .fullName("John Farmer")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        targetUser.setId(targetUserId);
    }

    // =========================================================================
    // 1. Pagination & Filtering Tests
    // =========================================================================

    @Test
    @DisplayName("getUsers - Should return paginated PageResponse<UserDetailDto> with linked identities and diagnoses count")
    void testGetUsers_WithFilterAndPagination_ReturnsPageResponse() {
        UserFilter filter = new UserFilter("john", Role.ROLE_USER, true);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(targetUser), pageable, 1);

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);

        UserIdentity identity = UserIdentity.builder()
                .user(targetUser)
                .provider(AuthProvider.LOCAL)
                .providerUserId("john@farm.vn")
                .build();
        when(userIdentityRepository.findByUserId(targetUserId)).thenReturn(List.of(identity));
        when(diagnosisRepository.countByUserIdAndDeletedAtIsNull(targetUserId)).thenReturn(5L);

        PageResponse<UserDetailDto> result = userService.getUsers(filter, pageable);

        assertThat(result).isNotNull();
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);

        UserDetailDto dto = result.content().get(0);
        assertThat(dto.id()).isEqualTo(targetUserId);
        assertThat(dto.username()).isEqualTo("farmer_john");
        assertThat(dto.email()).isEqualTo("john@farm.vn");
        assertThat(dto.role()).isEqualTo(Role.ROLE_USER);
        assertThat(dto.isActive()).isTrue();
        assertThat(dto.linkedProviders()).containsExactly("LOCAL");
        assertThat(dto.totalDiagnosesCount()).isEqualTo(5L);

        verify(userRepository).findAll(any(Specification.class), eq(pageable));
    }

    // =========================================================================
    // 2. User Detail Retrieval Tests
    // =========================================================================

    @Test
    @DisplayName("getUserDetail - Should return complete UserDetailDto when user exists")
    void testGetUserDetail_WhenUserExists_ReturnsUserDetailDto() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

        UserIdentity localIdentity = UserIdentity.builder()
                .user(targetUser)
                .provider(AuthProvider.LOCAL)
                .providerUserId("john@farm.vn")
                .build();
        UserIdentity googleIdentity = UserIdentity.builder()
                .user(targetUser)
                .provider(AuthProvider.GOOGLE)
                .providerUserId("google-sub-123")
                .build();
        when(userIdentityRepository.findByUserId(targetUserId)).thenReturn(List.of(localIdentity, googleIdentity));
        when(diagnosisRepository.countByUserIdAndDeletedAtIsNull(targetUserId)).thenReturn(12L);

        UserDetailDto detail = userService.getUserDetail(targetUserId);

        assertThat(detail).isNotNull();
        assertThat(detail.id()).isEqualTo(targetUserId);
        assertThat(detail.username()).isEqualTo("farmer_john");
        assertThat(detail.email()).isEqualTo("john@farm.vn");
        assertThat(detail.fullName()).isEqualTo("John Farmer");
        assertThat(detail.role()).isEqualTo(Role.ROLE_USER);
        assertThat(detail.isActive()).isTrue();
        assertThat(detail.linkedProviders()).containsExactlyInAnyOrder("LOCAL", "GOOGLE");
        assertThat(detail.totalDiagnosesCount()).isEqualTo(12L);
    }

    @Test
    @DisplayName("getUserDetail - Should throw BusinessException(USER_NOT_FOUND) when user does not exist")
    void testGetUserDetail_WhenUserNotFound_ThrowsBusinessException() {
        UUID nonExistentId = UUID.randomUUID();
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserDetail(nonExistentId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    // =========================================================================
    // 3. User Status Update & Self-Lockout Guard Tests
    // =========================================================================

    @Test
    @DisplayName("updateUserStatus - Should deactivate user and revoke all active refresh token sessions")
    void testUpdateUserStatus_DeactivateUser_RevokesRefreshTokensAndPersists() {
        UserStatusUpdateRequest request = new UserStatusUpdateRequest(false, "Suspicious activity detected");

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userIdentityRepository.findByUserId(targetUserId)).thenReturn(List.of());
        when(diagnosisRepository.countByUserIdAndDeletedAtIsNull(targetUserId)).thenReturn(0L);

        UserDetailDto updated = userService.updateUserStatus(targetUserId, request, adminId);

        assertThat(updated.isActive()).isFalse();
        assertThat(targetUser.getIsActive()).isFalse();

        verify(userRepository).save(targetUser);
        verify(refreshTokenStore).revokeAllForUser(targetUserId);
    }

    @Test
    @DisplayName("updateUserStatus - Should reactivate suspended user successfully")
    void testUpdateUserStatus_ReactivateUser_Success() {
        targetUser.setIsActive(false);
        UserStatusUpdateRequest request = new UserStatusUpdateRequest(true, "Account re-activated after review");

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userIdentityRepository.findByUserId(targetUserId)).thenReturn(List.of());
        when(diagnosisRepository.countByUserIdAndDeletedAtIsNull(targetUserId)).thenReturn(0L);

        UserDetailDto updated = userService.updateUserStatus(targetUserId, request, adminId);

        assertThat(updated.isActive()).isTrue();
        assertThat(targetUser.getIsActive()).isTrue();

        verify(userRepository).save(targetUser);
    }

    @Test
    @DisplayName("updateUserStatus - Self-Lockout Guard: Admin attempting to deactivate their own account must be rejected with CANNOT_MODIFY_OWN_ACCOUNT")
    void testUpdateUserStatus_SelfLockoutGuard_ThrowsCannotModifyOwnAccount() {
        UserStatusUpdateRequest request = new UserStatusUpdateRequest(false, "Attempting self-deactivation");

        assertThatThrownBy(() -> userService.updateUserStatus(adminId, request, adminId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CANNOT_MODIFY_OWN_ACCOUNT);
                });

        verify(userRepository, never()).save(any());
        verify(refreshTokenStore, never()).revokeAllForUser(any());
    }

    @Test
    @DisplayName("updateUserStatus - Should throw BusinessException(USER_NOT_FOUND) when target user does not exist")
    void testUpdateUserStatus_TargetUserNotFound_ThrowsBusinessException() {
        UUID nonExistentId = UUID.randomUUID();
        UserStatusUpdateRequest request = new UserStatusUpdateRequest(false, "Lock account");
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserStatus(nonExistentId, request, adminId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(userRepository, never()).save(any());
        verify(refreshTokenStore, never()).revokeAllForUser(any());
    }

    // =========================================================================
    // 4. User Role Update & Self-Demotion Guard Tests
    // =========================================================================

    @Test
    @DisplayName("updateUserRole - Should update user role successfully for another user")
    void testUpdateUserRole_PromoteUser_Success() {
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ROLE_EXPERT);

        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userIdentityRepository.findByUserId(targetUserId)).thenReturn(List.of());
        when(diagnosisRepository.countByUserIdAndDeletedAtIsNull(targetUserId)).thenReturn(0L);

        UserDetailDto updated = userService.updateUserRole(targetUserId, request, adminId);

        assertThat(updated.role()).isEqualTo(Role.ROLE_EXPERT);
        assertThat(targetUser.getRole()).isEqualTo(Role.ROLE_EXPERT);

        verify(userRepository).save(targetUser);
    }

    @Test
    @DisplayName("updateUserRole - Self-Demotion Guard: Admin attempting to modify their own role must be rejected with CANNOT_MODIFY_OWN_ACCOUNT")
    void testUpdateUserRole_SelfDemotionGuard_ThrowsCannotModifyOwnAccount() {
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ROLE_USER);

        assertThatThrownBy(() -> userService.updateUserRole(adminId, request, adminId))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(ErrorCode.CANNOT_MODIFY_OWN_ACCOUNT);
                });

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUserRole - Should throw BusinessException(USER_NOT_FOUND) when target user does not exist")
    void testUpdateUserRole_TargetUserNotFound_ThrowsBusinessException() {
        UUID nonExistentId = UUID.randomUUID();
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ROLE_EXPERT);
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateUserRole(nonExistentId, request, adminId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(userRepository, never()).save(any());
    }
}
