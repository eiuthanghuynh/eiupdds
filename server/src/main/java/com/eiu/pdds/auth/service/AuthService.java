package com.eiu.pdds.auth.service;

import com.eiu.pdds.auth.dto.AuthResponse;
import com.eiu.pdds.auth.dto.GoogleLoginRequest;
import com.eiu.pdds.auth.dto.LoginRequest;
import com.eiu.pdds.auth.dto.RefreshTokenRequest;
import com.eiu.pdds.auth.dto.RegisterRequest;
import com.eiu.pdds.auth.dto.UserSummaryDto;

import java.util.UUID;

/**
 * Service contract for User Authentication, Token Exchange, and Lifecycle management (SRP).
 */
public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse loginWithGoogle(GoogleLoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String refreshToken, UUID currentUserId);

    UserSummaryDto getCurrentUser(UUID currentUserId);
}
