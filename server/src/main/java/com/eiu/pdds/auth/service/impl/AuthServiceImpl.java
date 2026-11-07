package com.eiu.pdds.auth.service.impl;

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
import com.eiu.pdds.auth.service.AuthService;
import com.eiu.pdds.auth.service.RefreshTokenStore;
import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.user.entity.AuthProvider;
import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.entity.UserIdentity;
import com.eiu.pdds.user.repository.UserIdentityRepository;
import com.eiu.pdds.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Enterprise implementation of AuthService orchestrating Federated Identity registration,
 * credential verification, Google Token Exchange, and Refresh Token Rotation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Value("${jwt.refresh-token-expiration-ms:604800000}")
    private long refreshExpirationMs;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        String username = request.username().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "Email is already registered: " + normalizedEmail);
        }
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "Username is already taken: " + username);
        }

        String hashedPassword = passwordEncoder.encode(request.password());

        User user = User.builder()
                .username(username)
                .email(normalizedEmail)
                .passwordHash(hashedPassword)
                .fullName(request.fullName() != null ? request.fullName().trim() : username)
                .phoneNumber(request.phoneNumber() != null ? request.phoneNumber().trim() : null)
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        UserIdentity localIdentity = UserIdentity.builder()
                .user(savedUser)
                .provider(AuthProvider.LOCAL)
                .providerUserId(savedUser.getId().toString())
                .email(normalizedEmail)
                .build();

        userIdentityRepository.save(localIdentity);

        log.info("Registered new user: id={}, username={}, role=ROLE_USER", savedUser.getId(), savedUser.getUsername());

        return issueAuthTokens(savedUser, "Registration");
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.emailOrUsername().trim();

        User user = userRepository.findByEmail(identifier.toLowerCase())
                .or(() -> userRepository.findByUsername(identifier))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid username/email or password"));

        if (user.getPasswordHash() == null) {
            log.warn("Login attempt via password for third-party OAuth account: email={}", user.getEmail());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Account is registered with Google. Please use Google Sign-In.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Failed password authentication attempt for user={}", identifier);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid username/email or password");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BusinessException(ErrorCode.USER_INACTIVE, "User account is suspended");
        }

        return issueAuthTokens(user, request.deviceInfo());
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleUserPayload googlePayload = googleTokenVerifier.verify(request.idToken());

        String googleSub = googlePayload.subject();
        String googleEmail = googlePayload.email().toLowerCase();

        Optional<UserIdentity> existingIdentity = userIdentityRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, googleSub);

        User targetUser;

        if (existingIdentity.isPresent()) {
            targetUser = existingIdentity.get().getUser();
            if (targetUser.getAvatarUrl() == null && googlePayload.pictureUrl() != null) {
                targetUser.setAvatarUrl(googlePayload.pictureUrl());
                userRepository.save(targetUser);
            }
        } else {
            // Check if user with same email exists -> Auto-link identity
            Optional<User> userByEmail = userRepository.findByEmail(googleEmail);

            if (userByEmail.isPresent()) {
                targetUser = userByEmail.get();
                log.info("Auto-linking existing user account to Google identity: userId={}, email={}", targetUser.getId(), googleEmail);
                if (targetUser.getAvatarUrl() == null && googlePayload.pictureUrl() != null) {
                    targetUser.setAvatarUrl(googlePayload.pictureUrl());
                    userRepository.save(targetUser);
                }
            } else {
                // Register new user via Google
                String baseUsername = generateBaseUsername(googlePayload.name(), googleEmail);
                String uniqueUsername = resolveUniqueUsername(baseUsername);

                targetUser = User.builder()
                        .username(uniqueUsername)
                        .email(googleEmail)
                        .passwordHash(null)
                        .fullName(googlePayload.name() != null ? googlePayload.name() : uniqueUsername)
                        .avatarUrl(googlePayload.pictureUrl())
                        .role(Role.ROLE_USER)
                        .isActive(true)
                        .build();

                targetUser = userRepository.save(targetUser);
                log.info("Registered new user via Google Sign-In: id={}, email={}", targetUser.getId(), googleEmail);
            }

            UserIdentity googleIdentity = UserIdentity.builder()
                    .user(targetUser)
                    .provider(AuthProvider.GOOGLE)
                    .providerUserId(googleSub)
                    .email(googleEmail)
                    .build();

            userIdentityRepository.save(googleIdentity);
        }

        if (!Boolean.TRUE.equals(targetUser.getIsActive())) {
            throw new BusinessException(ErrorCode.USER_INACTIVE, "User account is suspended");
        }

        return issueAuthTokens(targetUser, request.deviceInfo());
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.refreshToken().trim();

        RefreshTokenSession session = refreshTokenStore.find(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN, "Refresh token is invalid or expired"));

        if (session.isConsumed()) {
            log.warn("Security Alert: Replay of consumed refresh token detected for userId={}. Revoking all sessions.", session.userId());
            refreshTokenStore.revokeAllForUser(session.userId());
            throw new BusinessException(ErrorCode.TOKEN_REUSE_DETECTED, "Security alert: token reuse detected, all active sessions terminated");
        }

        User user = userRepository.findById(session.userId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User associated with token not found"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BusinessException(ErrorCode.USER_INACTIVE, "User account is suspended");
        }

        String newRefreshTokenId = jwtTokenProvider.generateRefreshTokenId();
        boolean rotated = refreshTokenStore.rotate(token, newRefreshTokenId, Duration.ofMillis(refreshExpirationMs));

        if (!rotated) {
            log.warn("Security Alert: Token reuse detected during refresh for userId={}. Revoking all sessions.", user.getId());
            refreshTokenStore.revokeAllForUser(user.getId());
            throw new BusinessException(ErrorCode.TOKEN_REUSE_DETECTED, "Security alert: token reuse detected, all active sessions terminated");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        return AuthResponse.of(
                newAccessToken,
                newRefreshTokenId,
                jwtTokenProvider.getAccessTokenExpirationMs(),
                UserSummaryDto.fromEntity(user)
        );
    }

    @Override
    public void logout(String refreshToken, UUID currentUserId) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenStore.revoke(refreshToken.trim());
        } else if (currentUserId != null) {
            refreshTokenStore.revokeAllForUser(currentUserId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUser(UUID currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Unauthenticated user");
        }
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "User not found"));
        return UserSummaryDto.fromEntity(user);
    }

    private AuthResponse issueAuthTokens(User user, String deviceInfo) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenId = jwtTokenProvider.generateRefreshTokenId();

        refreshTokenStore.store(
                refreshTokenId,
                user.getId(),
                deviceInfo != null ? deviceInfo : "Unknown device",
                Duration.ofMillis(refreshExpirationMs)
        );

        return AuthResponse.of(
                accessToken,
                refreshTokenId,
                jwtTokenProvider.getAccessTokenExpirationMs(),
                UserSummaryDto.fromEntity(user)
        );
    }

    private String generateBaseUsername(String name, String email) {
        if (name != null && !name.isBlank()) {
            return name.trim().toLowerCase().replaceAll("[^a-z0-9]", "_");
        }
        int atIdx = email.indexOf('@');
        return atIdx > 0 ? email.substring(0, atIdx).replaceAll("[^a-z0-9]", "_") : "user";
    }

    private String resolveUniqueUsername(String base) {
        String candidate = base;
        int counter = 1;
        while (userRepository.existsByUsername(candidate)) {
            candidate = base + "_" + counter++;
        }
        return candidate;
    }
}
