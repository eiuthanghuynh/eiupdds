package com.eiu.pdds.auth.controller;

import com.eiu.pdds.auth.dto.GoogleLoginRequest;
import com.eiu.pdds.auth.dto.LoginRequest;
import com.eiu.pdds.auth.dto.RefreshTokenRequest;
import com.eiu.pdds.auth.dto.RegisterRequest;
import com.eiu.pdds.auth.model.GoogleUserPayload;
import com.eiu.pdds.auth.security.GoogleTokenVerifier;
import com.eiu.pdds.auth.security.JwtTokenProvider;
import com.eiu.pdds.integration.roboflow.RoboflowClient;
import com.eiu.pdds.storage.service.StorageService;
import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.repository.UserIdentityRepository;
import com.eiu.pdds.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private GoogleTokenVerifier googleTokenVerifier;

    @MockBean
    private RoboflowClient roboflowClient;

    @MockBean
    private StorageService storageService;

    @BeforeEach
    void cleanUp() {
        userIdentityRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should register new user, return tokens and user summary")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "farmer_bob",
                "bob@farm.vn",
                "StrongPass123",
                "Bob Farmer",
                "+84901112233"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.username", is("farmer_bob")))
                .andExpect(jsonPath("$.data.user.email", is("bob@farm.vn")))
                .andExpect(jsonPath("$.data.user.role", is("ROLE_USER")));

        assertThat(userRepository.existsByEmail("bob@farm.vn")).isTrue();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Should reject duplicate email with 400 Bad Request")
    void testRegisterDuplicateEmail() throws Exception {
        RegisterRequest request1 = new RegisterRequest("farmer_alice", "alice@farm.vn", "Pass123456", "Alice", null);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        RegisterRequest request2 = new RegisterRequest("farmer_alice2", "alice@farm.vn", "Pass123456", "Alice 2", null);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("AUTH_004")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should successfully authenticate with email or username")
    void testLoginSuccess() throws Exception {
        User user = User.builder()
                .username("green_thumb")
                .email("thumb@eiu.edu.vn")
                .passwordHash(passwordEncoder.encode("Password123"))
                .fullName("Green Thumb")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        userRepository.save(user);

        // Login via email
        LoginRequest loginByEmail = new LoginRequest("thumb@eiu.edu.vn", "Password123", "Pixel 8");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginByEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is("thumb@eiu.edu.vn")));

        // Login via username
        LoginRequest loginByUsername = new LoginRequest("green_thumb", "Password123", "Pixel 8");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginByUsername)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.username", is("green_thumb")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should return 401 for incorrect password")
    void testLoginInvalidCredentials() throws Exception {
        User user = User.builder()
                .username("user_test")
                .email("user_test@eiu.edu.vn")
                .passwordHash(passwordEncoder.encode("Password123"))
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        userRepository.save(user);

        LoginRequest badRequest = new LoginRequest("user_test@eiu.edu.vn", "WrongPassword", null);
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_002")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/google - Should authenticate or provision user via Google ID Token")
    void testGoogleLogin() throws Exception {
        GoogleUserPayload payload = new GoogleUserPayload(
                "google-oauth-100200300",
                "google.farmer@gmail.com",
                true,
                "Google Farmer",
                "https://lh3.googleusercontent.com/photo.jpg"
        );
        when(googleTokenVerifier.verify(anyString())).thenReturn(payload);

        GoogleLoginRequest request = new GoogleLoginRequest("mock-valid-google-id-token", "Expo Mobile Client");
        mockMvc.perform(post("/api/v1/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is("google.farmer@gmail.com")));

        assertThat(userRepository.existsByEmail("google.farmer@gmail.com")).isTrue();
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Should rotate token, issue new pair, and detect replay attacks")
    void testRefreshTokenRotationAndReuseDetection() throws Exception {
        RegisterRequest registerReq = new RegisterRequest("rotate_user", "rotate@farm.vn", "Pass123456", "Rotate User", null);
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        String initialRefreshToken = jsonNode.get("data").get("refreshToken").asText();

        // 1. First refresh -> Success and new token issued
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(initialRefreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andReturn();

        JsonNode refreshJsonNode = objectMapper.readTree(refreshResult.getResponse().getContentAsString());
        String secondRefreshToken = refreshJsonNode.get("data").get("refreshToken").asText();
        assertThat(secondRefreshToken).isNotEqualTo(initialRefreshToken);

        // 2. Replay initial consumed refresh token -> Token Reuse Detected (401 Unauthorized, AUTH_006)
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_006")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - Should return 401 without Bearer token, 200 with valid Bearer token")
    void testGetCurrentUserEndpoint() throws Exception {
        // Without Bearer token -> 401
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_001")));

        // Create user and token
        User user = User.builder()
                .username("me_user")
                .email("me@farm.vn")
                .passwordHash("someHash")
                .fullName("Me User")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        user = userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user);

        // With Bearer token -> 200
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("me@farm.vn")))
                .andExpect(jsonPath("$.data.username", is("me_user")));
    }

    @Test
    @DisplayName("RBAC: Regular user with ROLE_USER must receive 403 Forbidden on admin endpoint")
    void testRbacForbiddenForUserRole() throws Exception {
        User regularUser = User.builder()
                .username("farmer_user")
                .email("farmer@farm.vn")
                .passwordHash("someHash")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        regularUser = userRepository.save(regularUser);

        String userToken = jwtTokenProvider.generateAccessToken(regularUser);

        mockMvc.perform(post("/api/v1/diseases")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("AUTH_003")));
    }
}
