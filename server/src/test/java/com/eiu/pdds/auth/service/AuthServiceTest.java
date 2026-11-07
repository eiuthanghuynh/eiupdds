package com.eiu.pdds.auth.service;

import com.eiu.pdds.auth.dto.AuthResponse;
import com.eiu.pdds.auth.dto.GoogleLoginRequest;
import com.eiu.pdds.auth.dto.LoginRequest;
import com.eiu.pdds.auth.dto.RefreshTokenRequest;
import com.eiu.pdds.auth.dto.RegisterRequest;
import com.eiu.pdds.auth.dto.UserSummaryDto;
import com.eiu.pdds.auth.model.GoogleUserPayload;
import com.eiu.pdds.auth.model.RefreshTokenSession;
import com.eiu.pdds.auth.security.GoogleTokenVerifier;
import com.eiu.pdds.auth.security.JwtTokenProvider;
import com.eiu.pdds.auth.service.impl.AuthServiceImpl;
import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserIdentityRepository userIdentityRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 604800000L);

        sampleUser = User.builder()
                .username("john_doe")
                .email("john.doe@example.com")
                .passwordHash("hashed_password_123")
                .fullName("John Doe")
                .phoneNumber("+84987654321")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        sampleUser.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("register: Should successfully hash password, save user & local identity, and issue tokens")
    void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest(
                "john_doe",
                "john.doe@example.com",
                "SecureP@ss123",
                "John Doe",
                "+84987654321"
        );

        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("john_doe")).thenReturn(false);
        when(passwordEncoder.encode("SecureP@ss123")).thenReturn("hashed_password_123");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtTokenProvider.generateAccessToken(sampleUser)).thenReturn("jwt.access.token");
        when(jwtTokenProvider.generateRefreshTokenId()).thenReturn("refresh-uuid-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(900000L);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.accessToken()).isEqualTo("jwt.access.token");
        assertThat(response.refreshToken()).isEqualTo("refresh-uuid-token");
        assertThat(response.user().username()).isEqualTo("john_doe");

        verify(userIdentityRepository).save(any(UserIdentity.class));
        verify(refreshTokenStore).store(eq("refresh-uuid-token"), eq(sampleUser.getId()), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("register: Should reject duplicate email with USER_ALREADY_EXISTS")
    void testRegisterDuplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "new_user",
                "john.doe@example.com",
                "SecureP@ss123",
                "New User",
                null
        );

        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.USER_ALREADY_EXISTS));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: Should reject duplicate username with USER_ALREADY_EXISTS")
    void testRegisterDuplicateUsername() {
        RegisterRequest request = new RegisterRequest(
                "john_doe",
                "other.email@example.com",
                "SecureP@ss123",
                "John Doe",
                null
        );

        when(userRepository.existsByEmail("other.email@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("john_doe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.USER_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("login: Should successfully authenticate with correct email and password")
    void testLoginWithEmailSuccess() {
        LoginRequest request = new LoginRequest("john.doe@example.com", "SecureP@ss123", "Pixel 8");

        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("SecureP@ss123", sampleUser.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(sampleUser)).thenReturn("jwt.access.token");
        when(jwtTokenProvider.generateRefreshTokenId()).thenReturn("refresh-token-id");

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("jwt.access.token");
        assertThat(response.user().email()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("login: Should reject invalid password with INVALID_CREDENTIALS")
    void testLoginInvalidPassword() {
        LoginRequest request = new LoginRequest("john.doe@example.com", "WrongPassword", null);

        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("WrongPassword", sampleUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }

    @Test
    @DisplayName("login: Should reject password login for Google OAuth account without password")
    void testLoginOAuthAccountWithPassword() {
        User oauthUser = User.builder()
                .username("google_user")
                .email("google.user@example.com")
                .passwordHash(null)
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        oauthUser.setId(UUID.randomUUID());

        LoginRequest request = new LoginRequest("google.user@example.com", "SomePass", null);

        when(userRepository.findByEmail("google.user@example.com")).thenReturn(Optional.of(oauthUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS);
                    assertThat(ex.getMessage()).contains("Google");
                });
    }

    @Test
    @DisplayName("loginWithGoogle: Should login existing Google identity")
    void testLoginWithGoogleExistingIdentity() {
        GoogleLoginRequest request = new GoogleLoginRequest("valid.google.id.token", "Chrome/Android");
        GoogleUserPayload payload = new GoogleUserPayload("google-sub-12345", "john.doe@example.com", true, "John Doe", "https://avatar.url");

        UserIdentity existingIdentity = UserIdentity.builder()
                .user(sampleUser)
                .provider(AuthProvider.GOOGLE)
                .providerUserId("google-sub-12345")
                .email("john.doe@example.com")
                .build();
        existingIdentity.setId(UUID.randomUUID());

        when(googleTokenVerifier.verify("valid.google.id.token")).thenReturn(payload);
        when(userIdentityRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub-12345"))
                .thenReturn(Optional.of(existingIdentity));
        when(jwtTokenProvider.generateAccessToken(sampleUser)).thenReturn("jwt.access.token");
        when(jwtTokenProvider.generateRefreshTokenId()).thenReturn("refresh-token-id");

        AuthResponse response = authService.loginWithGoogle(request);

        assertThat(response.user().email()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("loginWithGoogle: Should auto-link Google identity to existing local account with same email")
    void testLoginWithGoogleAutoLink() {
        GoogleLoginRequest request = new GoogleLoginRequest("valid.google.id.token", "iOS App");
        GoogleUserPayload payload = new GoogleUserPayload("google-sub-9999", "john.doe@example.com", true, "John Doe", null);

        when(googleTokenVerifier.verify("valid.google.id.token")).thenReturn(payload);
        when(userIdentityRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "google-sub-9999"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(sampleUser));
        when(jwtTokenProvider.generateAccessToken(sampleUser)).thenReturn("jwt.access.token");
        when(jwtTokenProvider.generateRefreshTokenId()).thenReturn("refresh-token-id");

        AuthResponse response = authService.loginWithGoogle(request);

        assertThat(response.user().id()).isEqualTo(sampleUser.getId());
        ArgumentCaptor<UserIdentity> captor = ArgumentCaptor.forClass(UserIdentity.class);
        verify(userIdentityRepository).save(captor.capture());
        assertThat(captor.getValue().getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(captor.getValue().getProviderUserId()).isEqualTo("google-sub-9999");
    }

    @Test
    @DisplayName("refreshToken: Should rotate token and issue new access token")
    void testRefreshTokenSuccess() {
        RefreshTokenRequest request = new RefreshTokenRequest("active-refresh-token");
        long now = System.currentTimeMillis();
        RefreshTokenSession session = new RefreshTokenSession("active-refresh-token", sampleUser.getId(), "Pixel 8", now, now + 604800000L);

        when(refreshTokenStore.find("active-refresh-token")).thenReturn(Optional.of(session));
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(jwtTokenProvider.generateRefreshTokenId()).thenReturn("new-refresh-token");
        when(refreshTokenStore.rotate(eq("active-refresh-token"), eq("new-refresh-token"), any(Duration.class))).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(sampleUser)).thenReturn("new.access.token");

        AuthResponse response = authService.refreshToken(request);

        assertThat(response.accessToken()).isEqualTo("new.access.token");
        assertThat(response.refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("refreshToken: Should detect token reuse attack, terminate all user sessions, and throw TOKEN_REUSE_DETECTED")
    void testRefreshTokenReuseAttackDetection() {
        RefreshTokenRequest request = new RefreshTokenRequest("stolen-or-replayed-token");
        long now = System.currentTimeMillis();
        RefreshTokenSession session = new RefreshTokenSession("stolen-or-replayed-token", sampleUser.getId(), "Stolen Device", now, now + 604800000L);

        when(refreshTokenStore.find("stolen-or-replayed-token")).thenReturn(Optional.of(session));
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));
        when(jwtTokenProvider.generateRefreshTokenId()).thenReturn("candidate-token");
        // rotate returns false -> token was already rotated/consumed!
        when(refreshTokenStore.rotate(eq("stolen-or-replayed-token"), eq("candidate-token"), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.TOKEN_REUSE_DETECTED));

        verify(refreshTokenStore).revokeAllForUser(sampleUser.getId());
    }

    @Test
    @DisplayName("logout: Should revoke specific refresh token when provided")
    void testLogoutWithToken() {
        authService.logout("token-to-revoke", null);
        verify(refreshTokenStore).revoke("token-to-revoke");
    }

    @Test
    @DisplayName("logout: Should revoke all user sessions when only userId is provided")
    void testLogoutWithUserIdOnly() {
        UUID userId = UUID.randomUUID();
        authService.logout(null, userId);
        verify(refreshTokenStore).revokeAllForUser(userId);
    }

    @Test
    @DisplayName("getCurrentUser: Should return user profile for authenticated user ID")
    void testGetCurrentUserSuccess() {
        when(userRepository.findById(sampleUser.getId())).thenReturn(Optional.of(sampleUser));

        UserSummaryDto userSummary = authService.getCurrentUser(sampleUser.getId());

        assertThat(userSummary.id()).isEqualTo(sampleUser.getId());
        assertThat(userSummary.username()).isEqualTo("john_doe");
        assertThat(userSummary.email()).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("getCurrentUser: Should throw UNAUTHORIZED if user ID is null")
    void testGetCurrentUserNullId() {
        assertThatThrownBy(() -> authService.getCurrentUser(null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }
}
