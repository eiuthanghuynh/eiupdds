package com.eiu.pdds.common.ratelimit;

import com.eiu.pdds.auth.dto.LoginRequest;
import com.eiu.pdds.auth.security.JwtTokenProvider;
import com.eiu.pdds.diagnosis.repository.BoundingBoxRepository;
import com.eiu.pdds.diagnosis.repository.DiagnosisRepository;
import com.eiu.pdds.disease.entity.Disease;
import com.eiu.pdds.disease.entity.Severity;
import com.eiu.pdds.disease.repository.DiseaseRepository;
import com.eiu.pdds.integration.roboflow.RoboflowClient;
import com.eiu.pdds.integration.roboflow.dto.RoboflowWorkflowResponse;
import com.eiu.pdds.plant.entity.Plant;
import com.eiu.pdds.plant.repository.PlantRepository;
import com.eiu.pdds.storage.service.StorageService;
import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "pdds.rate-limit.enabled=true")
class RateLimitIntegrationTest {

    private static final String ANALYZE_URI = "/api/v1/diagnoses/analyze";
    private static final String AUTH_LOGIN_URI = "/api/v1/auth/login";
    private static final String AUTH_ME_URI = "/api/v1/auth/me";

    private static final String USER_A_USERNAME = "rl_user_a";
    private static final String USER_A_EMAIL = "rl_user_a@eiu.edu.vn";
    private static final String USER_B_USERNAME = "rl_user_b";
    private static final String USER_B_EMAIL = "rl_user_b@eiu.edu.vn";
    private static final String ADMIN_USERNAME = "rl_admin";
    private static final String ADMIN_EMAIL = "rl_admin@eiu.edu.vn";
    private static final String EXPERT_USERNAME = "rl_expert";
    private static final String EXPERT_EMAIL = "rl_expert@eiu.edu.vn";

    private static final List<String> TEST_USER_EMAILS = List.of(
            USER_A_EMAIL,
            USER_B_EMAIL,
            ADMIN_EMAIL,
            EXPERT_EMAIL
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryRateLimiter inMemoryRateLimiter;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlantRepository plantRepository;

    @Autowired
    private DiseaseRepository diseaseRepository;

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Autowired
    private BoundingBoxRepository boundingBoxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoboflowClient roboflowClient;

    @MockBean
    private StorageService storageService;

    private final AtomicInteger imageSequence = new AtomicInteger(1);

    private User userA;
    private User userB;
    private User adminUser;
    private User expertUser;

    private String userAToken;
    private String userBToken;
    private String adminToken;
    private String expertToken;

    @BeforeEach
    void setUp() {
        inMemoryRateLimiter.clear();
        Mockito.reset(storageService, roboflowClient);

        when(storageService.uploadImage(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
                .thenReturn("https://s3.eiu.edu.vn/eiu-pdds-plants/rate_limit_leaf.jpg");

        RoboflowWorkflowResponse.ModelOutput mockOutput = new RoboflowWorkflowResponse.ModelOutput(
                new RoboflowWorkflowResponse.ImageInfo(400, 500),
                List.of(
                        new RoboflowWorkflowResponse.Prediction(200.0, 250.0, 100.0, 100.0, 0.88, 13, "Potato leaf late blight", "det-1", "image")
                )
        );
        when(roboflowClient.inferPlantDisease(ArgumentMatchers.<byte[]>any(), ArgumentMatchers.nullable(Double.class)))
                .thenReturn(mockOutput);

        boundingBoxRepository.deleteAll();
        diagnosisRepository.deleteAll();

        plantRepository.findByPlantCode("POTATO")
                .orElseGet(() -> plantRepository.save(Plant.builder()
                        .plantCode("POTATO")
                        .commonName("Potato")
                        .scientificName("Solanum tuberosum")
                        .iconUrl("https://cdn.eiu.edu.vn/icons/potato.svg")
                        .description("Tuber crop")
                        .build()));

        diseaseRepository.findByDiseaseCode("POTATO_LATE_BLIGHT")
                .orElseGet(() -> diseaseRepository.save(Disease.builder()
                        .diseaseCode("POTATO_LATE_BLIGHT")
                        .commonName("Potato late blight")
                        .scientificName("Phytophthora infestans")
                        .severity(Severity.CRITICAL)
                        .symptoms("Dark brown to black water-soaked lesions.")
                        .description("Destructive fungal-like oomycete disease.")
                        .build()));

        String encodedPassword = passwordEncoder.encode("RateLimitPass123!");
        userA = createOrGetTestUser(USER_A_USERNAME, USER_A_EMAIL, encodedPassword, Role.ROLE_USER);
        userB = createOrGetTestUser(USER_B_USERNAME, USER_B_EMAIL, encodedPassword, Role.ROLE_USER);
        adminUser = createOrGetTestUser(ADMIN_USERNAME, ADMIN_EMAIL, encodedPassword, Role.ROLE_ADMIN);
        expertUser = createOrGetTestUser(EXPERT_USERNAME, EXPERT_EMAIL, encodedPassword, Role.ROLE_EXPERT);

        userAToken = jwtTokenProvider.generateAccessToken(userA);
        userBToken = jwtTokenProvider.generateAccessToken(userB);
        adminToken = jwtTokenProvider.generateAccessToken(adminUser);
        expertToken = jwtTokenProvider.generateAccessToken(expertUser);
    }

    @AfterEach
    void tearDown() {
        inMemoryRateLimiter.clear();
        boundingBoxRepository.deleteAll();
        diagnosisRepository.deleteAll();
        for (String email : TEST_USER_EMAILS) {
            userRepository.findByEmail(email).ifPresent(userRepository::delete);
        }
    }

    // =========================================================================
    // 1. Guest Diagnosis Rate Limiting (3 req / 3600s per IP)
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/diagnoses/analyze - Guest IP allows 3 requests and blocks 4th with 429 RATE_001")
    void guestAnalyze_enforces3RequestsPerHourPerIp() throws Exception {
        String guestIp = "198.51.100.10";

        for (int attempt = 0; attempt < 3; attempt++) {
            MvcResult result = performAnalyze(guestIp, null, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andReturn();
            assertAllowedRateLimitHeaders(result, 3, 2 - attempt);
        }

        MvcResult blockedResult = performAnalyze(guestIp, null, null)
                .andReturn();
        assertRateLimitExceededResponse(blockedResult, 3, ANALYZE_URI, 3500L);
    }

    // =========================================================================
    // 2. Authenticated ROLE_USER Rate Limiting (10 req / 60s per userId)
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/diagnoses/analyze - ROLE_USER allows 10 req/min per userId and isolates across users on same IP")
    void authenticatedUserAnalyze_enforces10RequestsPerMinuteAndIsolatesUsers() throws Exception {
        String sharedIp = "198.51.100.11";

        for (int attempt = 0; attempt < 10; attempt++) {
            MvcResult result = performAnalyze(sharedIp, userAToken, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andReturn();
            assertAllowedRateLimitHeaders(result, 10, 9 - attempt);
        }

        MvcResult blockedUserAResult = performAnalyze(sharedIp, userAToken, null)
                .andReturn();
        assertRateLimitExceededResponse(blockedUserAResult, 10, ANALYZE_URI, 1L);

        // User B shares the same remote IP but has an independent userId bucket
        MvcResult userBResult = performAnalyze(sharedIp, userBToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andReturn();
        assertAllowedRateLimitHeaders(userBResult, 10, 9);
    }

    // =========================================================================
    // 3. ROLE_ADMIN & ROLE_EXPERT Bypass
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/diagnoses/analyze - ROLE_ADMIN and ROLE_EXPERT bypass rate limiting")
    void privilegedRoles_bypassDiagnosisRateLimit() throws Exception {
        String privilegedIp = "198.51.100.12";

        for (int i = 0; i < 12; i++) {
            performAnalyze(privilegedIp, adminToken, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));
        }

        for (int i = 0; i < 12; i++) {
            performAnalyze(privilegedIp, expertToken, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));
        }
    }

    // =========================================================================
    // 4. Auth Brute-Force Protection & GET /api/v1/auth/me Exemption
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/auth/login - Enforces 10 req/60s per IP while GET /api/v1/auth/me remains exempt")
    void authLogin_enforces10RequestsPerMinutePerIp_andGetMeIsExempt() throws Exception {
        String attackerIp = "198.51.100.20";
        String invalidLoginJson = objectMapper.writeValueAsString(
                new LoginRequest(userA.getEmail(), "WrongPassword999!", "RateLimitIntegrationTest")
        );

        for (int attempt = 0; attempt < 10; attempt++) {
            MvcResult result = mockMvc.perform(post(AUTH_LOGIN_URI)
                            .with(remoteAddr(attackerIp))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidLoginJson))
                    .andExpect(status().isUnauthorized())
                    .andReturn();
            assertAllowedRateLimitHeaders(result, 10, 9 - attempt);
        }

        MvcResult blockedLoginResult = mockMvc.perform(post(AUTH_LOGIN_URI)
                        .with(remoteAddr(attackerIp))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidLoginJson))
                .andReturn();
        assertRateLimitExceededResponse(blockedLoginResult, 10, AUTH_LOGIN_URI, 1L);

        // GET /api/v1/auth/me from the same exhausted IP must succeed without rate limiting
        mockMvc.perform(get(AUTH_ME_URI)
                        .with(remoteAddr(attackerIp))
                        .header("Authorization", "Bearer " + userAToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is(USER_A_EMAIL)));
    }

    // =========================================================================
    // 5. Anti-Spoofing (Untrusted X-Forwarded-For Ignored)
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/diagnoses/analyze - Ignores forged X-Forwarded-For headers from untrusted remoteAddr")
    void guestAnalyze_ignoresForgedXForwardedForFromUntrustedPeer() throws Exception {
        String untrustedPeerIp = "198.51.100.30";
        List<String> forgedForwardedIps = List.of("1.1.1.1", "2.2.2.2", "3.3.3.3", "4.4.4.4");

        for (int i = 0; i < 3; i++) {
            MvcResult result = performAnalyze(untrustedPeerIp, null, forgedForwardedIps.get(i))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andReturn();
            assertAllowedRateLimitHeaders(result, 3, 2 - i);
        }

        MvcResult blockedResult = performAnalyze(untrustedPeerIp, null, forgedForwardedIps.get(3))
                .andReturn();
        assertRateLimitExceededResponse(blockedResult, 3, ANALYZE_URI, 3500L);
    }

    // =========================================================================
    // 6. Concurrent Burst Enforcement (ROLE_USER)
    // =========================================================================

    @Test
    @DisplayName("POST /api/v1/diagnoses/analyze - Concurrent 15-thread burst allows strictly 10 and rejects 5 with 429")
    void authenticatedUserAnalyze_concurrentBurst_allowsStrictly10AndRejects5() throws Exception {
        int totalThreads = 15;
        String burstIp = "198.51.100.40";
        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch readyLatch = new CountDownLatch(totalThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>(totalThreads);

        try {
            for (int i = 0; i < totalThreads; i++) {
                Callable<Integer> task = () -> {
                    readyLatch.countDown();
                    assertThat(startLatch.await(10, TimeUnit.SECONDS)).isTrue();
                    MvcResult result = performAnalyze(burstIp, userAToken, null).andReturn();
                    return result.getResponse().getStatus();
                };
                futures.add(executor.submit(task));
            }

            assertThat(readyLatch.await(10, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();

            int okCount = 0;
            int tooManyRequestsCount = 0;
            List<Integer> unexpectedStatuses = new ArrayList<>();

            for (Future<Integer> future : futures) {
                int statusCode = future.get(30, TimeUnit.SECONDS);
                if (statusCode == 200) {
                    okCount++;
                } else if (statusCode == 429) {
                    tooManyRequestsCount++;
                } else {
                    unexpectedStatuses.add(statusCode);
                }
            }

            assertThat(unexpectedStatuses).isEmpty();
            assertThat(okCount).isEqualTo(10);
            assertThat(tooManyRequestsCount).isEqualTo(5);
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private User createOrGetTestUser(String username, String email, String encodedPassword, Role role) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.builder()
                        .username(username)
                        .email(email)
                        .passwordHash(encodedPassword)
                        .fullName(username.toUpperCase())
                        .role(role)
                        .isActive(true)
                        .build()));
    }

    private ResultActions performAnalyze(
            String remoteIp,
            String bearerToken,
            String forwardedForHeader
    ) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = multipart(ANALYZE_URI)
                .file(createUniqueValidJpegFile())
                .param("plantCode", "POTATO")
                .param("latitude", "11.0528")
                .param("longitude", "106.6661")
                .param("deviceInfo", "RateLimitIntegrationTest")
                .with(remoteAddr(remoteIp));

        if (bearerToken != null) {
            requestBuilder.header("Authorization", "Bearer " + bearerToken);
        }
        if (forwardedForHeader != null) {
            requestBuilder.header("X-Forwarded-For", forwardedForHeader);
        }
        return mockMvc.perform(requestBuilder);
    }

    private MockMultipartFile createUniqueValidJpegFile() {
        int seq = imageSequence.getAndIncrement();
        byte[] jpegBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01,
                (byte) ((seq >>> 24) & 0xFF),
                (byte) ((seq >>> 16) & 0xFF),
                (byte) ((seq >>> 8) & 0xFF),
                (byte) (seq & 0xFF)
        };
        return new MockMultipartFile(
                "image",
                "leaf_" + seq + ".jpg",
                MediaType.IMAGE_JPEG_VALUE,
                jpegBytes
        );
    }

    private RequestPostProcessor remoteAddr(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private void assertAllowedRateLimitHeaders(MvcResult result, int expectedLimit, int expectedRemaining) {
        String limitHeader = result.getResponse().getHeader("X-RateLimit-Limit");
        String remainingHeader = result.getResponse().getHeader("X-RateLimit-Remaining");
        String resetHeader = result.getResponse().getHeader("X-RateLimit-Reset");

        assertThat(limitHeader).isEqualTo(String.valueOf(expectedLimit));
        assertThat(remainingHeader).isEqualTo(String.valueOf(expectedRemaining));
        assertThat(resetHeader).isNotBlank();
        assertThat(Long.parseLong(resetHeader)).isPositive();
    }

    private void assertRateLimitExceededResponse(
            MvcResult result,
            int expectedLimit,
            String expectedPath,
            long minRetryAfterSeconds
    ) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(result.getResponse().getHeader("X-RateLimit-Limit")).isEqualTo(String.valueOf(expectedLimit));
        assertThat(result.getResponse().getHeader("X-RateLimit-Remaining")).isEqualTo("0");

        String resetHeader = result.getResponse().getHeader("X-RateLimit-Reset");
        assertThat(resetHeader).isNotBlank();
        assertThat(Long.parseLong(resetHeader)).isPositive();

        String retryAfterHeader = result.getResponse().getHeader("Retry-After");
        assertThat(retryAfterHeader).isNotBlank();
        assertThat(Long.parseLong(retryAfterHeader)).isGreaterThanOrEqualTo(minRetryAfterSeconds);

        String responseBody = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode json = objectMapper.readTree(responseBody);
        assertThat(json.path("success").asBoolean()).isFalse();
        assertThat(json.path("code").asText()).isEqualTo("RATE_001");
        assertThat(json.path("message").asText()).isNotBlank();
        assertThat(json.path("timestamp").asText()).isNotBlank();
        assertThat(json.path("path").asText()).isEqualTo(expectedPath);
    }
}
