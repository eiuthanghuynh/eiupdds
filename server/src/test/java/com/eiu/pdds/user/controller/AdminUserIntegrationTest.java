package com.eiu.pdds.user.controller;

import com.eiu.pdds.auth.dto.LoginRequest;
import com.eiu.pdds.auth.dto.RefreshTokenRequest;
import com.eiu.pdds.auth.security.JwtTokenProvider;
import com.eiu.pdds.auth.service.RefreshTokenStore;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.integration.roboflow.RoboflowClient;
import com.eiu.pdds.storage.service.StorageService;
import com.eiu.pdds.user.dto.UserRoleUpdateRequest;
import com.eiu.pdds.user.dto.UserStatusUpdateRequest;
import com.eiu.pdds.user.entity.AuthProvider;
import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.entity.UserIdentity;
import com.eiu.pdds.user.repository.UserIdentityRepository;
import com.eiu.pdds.user.repository.UserRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration Test suite for {@link AdminUserController} (/api/v1/admin/users).
 *
 * <p>Validates RBAC security, administrative pagination, user detail inspection,
 * account suspension/deactivation with token invalidation, role mutation, and Self-Lockout Guard.
 *
 * <p>Phase: TDD RED Phase (Task 4.1).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private RefreshTokenStore refreshTokenStore;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private RoboflowClient roboflowClient;

    @MockBean
    private StorageService storageService;

    private User adminUser;
    private User expertUser;
    private User normalUser;
    private User targetManagedUser;

    private String adminToken;
    private String expertToken;
    private String userToken;

    @BeforeEach
    void setUp() {
        adminUser = createOrGetUser("admin_boss", "admin_boss@eiu.edu.vn", "AdminBoss123", Role.ROLE_ADMIN, true);
        expertUser = createOrGetUser("expert_agri", "expert_agri@eiu.edu.vn", "ExpertAgri123", Role.ROLE_EXPERT, true);
        normalUser = createOrGetUser("farmer_bob", "farmer_bob@eiu.edu.vn", "FarmerBob123", Role.ROLE_USER, true);
        targetManagedUser = createOrGetUser("target_farmer", "target_farmer@eiu.edu.vn", "TargetPass123", Role.ROLE_USER, true);

        adminToken = jwtTokenProvider.generateAccessToken(adminUser);
        expertToken = jwtTokenProvider.generateAccessToken(expertUser);
        userToken = jwtTokenProvider.generateAccessToken(normalUser);

        // Bind local identity for target managed user
        userIdentityRepository.findByProviderAndProviderUserId(AuthProvider.LOCAL, targetManagedUser.getEmail())
                .orElseGet(() -> userIdentityRepository.save(UserIdentity.builder()
                        .user(targetManagedUser)
                        .provider(AuthProvider.LOCAL)
                        .providerUserId(targetManagedUser.getEmail())
                        .email(targetManagedUser.getEmail())
                        .build()));
    }

    private User createOrGetUser(String username, String email, String rawPassword, Role role, boolean isActive) {
        return userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(User.builder()
                        .username(username)
                        .email(email)
                        .passwordHash(passwordEncoder.encode(rawPassword))
                        .fullName(username.replace('_', ' ').toUpperCase())
                        .role(role)
                        .isActive(isActive)
                        .build()));
    }

    // =========================================================================
    // 1. RBAC & Access Control Tests
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/admin/users - Missing Bearer token returns 401 Unauthorized")
    void testGetUsers_Unauthenticated_Returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_001")));
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - Calling with ROLE_USER returns 403 Forbidden")
    void testGetUsers_AsUserRole_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("AUTH_003")));
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - Calling with ROLE_EXPERT returns 403 Forbidden")
    void testGetUsers_AsExpertRole_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + expertToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("AUTH_003")));
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - Calling with ROLE_ADMIN returns 200 OK and paginated PageResponse")
    void testGetUsers_AsAdmin_Returns200WithPageResponse() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()))
                .andExpect(jsonPath("$.data.content.length()", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data.page", is(0)));
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - Filters users by keyword, role, and active status")
    void testGetUsers_WithFilters_ReturnsFilteredUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("keyword", "target_farmer")
                        .param("role", "ROLE_USER")
                        .param("isActive", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content.length()", is(1)))
                .andExpect(jsonPath("$.data.content[0].username", is("target_farmer")))
                .andExpect(jsonPath("$.data.content[0].role", is("ROLE_USER")))
                .andExpect(jsonPath("$.data.content[0].isActive", is(true)));
    }

    // =========================================================================
    // 2. User Detail Inspection Tests
    // =========================================================================

    @Test
    @DisplayName("GET /api/v1/admin/users/{id} - Calling as Admin returns full UserDetailDto")
    void testGetUserDetail_AsAdmin_ReturnsUserDetail() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/" + targetManagedUser.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(targetManagedUser.getId().toString())))
                .andExpect(jsonPath("$.data.username", is("target_farmer")))
                .andExpect(jsonPath("$.data.email", is("target_farmer@eiu.edu.vn")))
                .andExpect(jsonPath("$.data.role", is("ROLE_USER")))
                .andExpect(jsonPath("$.data.isActive", is(true)))
                .andExpect(jsonPath("$.data.linkedProviders", hasItem("LOCAL")))
                .andExpect(jsonPath("$.data.totalDiagnosesCount", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/admin/users/{id} - Calling as Non-Admin returns 403 Forbidden")
    void testGetUserDetail_AsUser_Returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/" + targetManagedUser.getId())
                        .header("Authorization", "Bearer " + userToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("AUTH_003")));
    }

    @Test
    @DisplayName("GET /api/v1/admin/users/{id} - Non-existent ID returns 404 Not Found")
    void testGetUserDetail_NotFound_Returns404() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/admin/users/" + nonExistentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("AUTH_008")));
    }

    // =========================================================================
    // 3. User Status Update & Token Invalidation Tests
    // =========================================================================

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/status - Admin deactivates user: updates isActive to false, blocks existing access token, login & refresh")
    void testUpdateUserStatus_LockUser_SuccessAndBlocksSubsequentAuth() throws Exception {
        // Pre-issue an active access token and refresh token session for targetManagedUser
        String targetAccessToken = jwtTokenProvider.generateAccessToken(targetManagedUser);
        String targetRefreshToken = jwtTokenProvider.generateRefreshTokenId();
        refreshTokenStore.store(targetRefreshToken, targetManagedUser.getId(), "Test Device", Duration.ofDays(7));

        UserStatusUpdateRequest request = new UserStatusUpdateRequest(false, "Repeated spam violations");

        // 1. Admin deactivates user
        mockMvc.perform(patch("/api/v1/admin/users/" + targetManagedUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.isActive", is(false)));

        // Verify entity updated in DB
        User updatedInDb = userRepository.findById(targetManagedUser.getId()).orElseThrow();
        assertThat(updatedInDb.getIsActive()).isFalse();

        // 2. Previously issued access token must be rejected immediately with 401 Unauthorized
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + targetAccessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_001")));

        // 3. Subsequent login attempt with correct credentials must fail with 403 USER_INACTIVE (AUTH_009)
        LoginRequest loginRequest = new LoginRequest("target_farmer", "TargetPass123", "Pixel 8");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("AUTH_009")));

        // 4. Subsequent refresh attempt must be rejected with 401 INVALID_REFRESH_TOKEN
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(targetRefreshToken);
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_005")));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/status - Self-Lockout Guard: Admin deactivating their own account returns 400 Bad Request (AUTH_010)")
    void testUpdateUserStatus_SelfLockoutGuard_Returns400BadRequest() throws Exception {
        UserStatusUpdateRequest selfLockoutRequest = new UserStatusUpdateRequest(false, "Accidental self lockout");

        mockMvc.perform(patch("/api/v1/admin/users/" + adminUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfLockoutRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("AUTH_010")));

        // Verify admin in DB is STILL active
        User adminInDb = userRepository.findById(adminUser.getId()).orElseThrow();
        assertThat(adminInDb.getIsActive()).isTrue();
    }

    // =========================================================================
    // 4. User Role Update & Self-Demotion Guard Tests
    // =========================================================================

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/role - Admin promotes user to ROLE_EXPERT successfully")
    void testUpdateUserRole_PromoteUser_Success() throws Exception {
        UserRoleUpdateRequest request = new UserRoleUpdateRequest(Role.ROLE_EXPERT);

        mockMvc.perform(patch("/api/v1/admin/users/" + targetManagedUser.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.role", is("ROLE_EXPERT")));

        User updatedInDb = userRepository.findById(targetManagedUser.getId()).orElseThrow();
        assertThat(updatedInDb.getRole()).isEqualTo(Role.ROLE_EXPERT);
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/role - Demoted admin immediately loses admin access with existing JWT and refresh tokens are revoked")
    void testUpdateUserRole_DemoteAdmin_ImmediatelyBlocksExistingAdminTokenAndRevokesRefresh() throws Exception {
        User otherAdmin = createOrGetUser("other_admin", "other_admin@eiu.edu.vn", "OtherAdmin123", Role.ROLE_ADMIN, true);
        String oldAdminAccessToken = jwtTokenProvider.generateAccessToken(otherAdmin);
        String oldAdminRefreshToken = jwtTokenProvider.generateRefreshTokenId();
        refreshTokenStore.store(oldAdminRefreshToken, otherAdmin.getId(), "Admin Browser", Duration.ofDays(7));

        // 1. Before demotion, oldAdminAccessToken can access admin endpoint
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + oldAdminAccessToken))
                .andExpect(status().isOk());

        // 2. Super admin demotes otherAdmin to ROLE_USER
        UserRoleUpdateRequest demoteRequest = new UserRoleUpdateRequest(Role.ROLE_USER);
        mockMvc.perform(patch("/api/v1/admin/users/" + otherAdmin.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demoteRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role", is("ROLE_USER")));

        // 3. Existing access token must IMMEDIATELY receive 403 Forbidden on admin endpoint
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + oldAdminAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("AUTH_003")));

        // 4. Existing access token still reflects updated ROLE_USER on /api/v1/auth/me
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + oldAdminAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role", is("ROLE_USER")));

        // 5. Existing refresh token must have been revoked
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(oldAdminRefreshToken);
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("AUTH_005")));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/role - Self-Demotion Guard: Admin demoting own role returns 400 Bad Request (AUTH_010)")
    void testUpdateUserRole_SelfDemotionGuard_Returns400BadRequest() throws Exception {
        UserRoleUpdateRequest selfDemotionRequest = new UserRoleUpdateRequest(Role.ROLE_USER);

        mockMvc.perform(patch("/api/v1/admin/users/" + adminUser.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfDemotionRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("AUTH_010")));

        // Verify admin role in DB remains ROLE_ADMIN
        User adminInDb = userRepository.findById(adminUser.getId()).orElseThrow();
        assertThat(adminInDb.getRole()).isEqualTo(Role.ROLE_ADMIN);
    }

    // =========================================================================
    // 5. Payload Validation Tests
    // =========================================================================

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/status - Missing required isActive field returns 400 Bad Request")
    void testUpdateUserStatus_ValidationFailure_MissingIsActive() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/" + targetManagedUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"no isActive\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/users/{id}/role - Missing required role field returns 400 Bad Request")
    void testUpdateUserRole_ValidationFailure_MissingRole() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/users/" + targetManagedUser.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
