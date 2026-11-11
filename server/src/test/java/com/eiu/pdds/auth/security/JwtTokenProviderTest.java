package com.eiu.pdds.auth.security;

import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET_256_BITS = "01234567890123456789012345678901"; // 32 chars = 256 bits
    private static final String ISSUER = "eiu-pdds-auth-test";
    private static final String AUDIENCE = "eiu-pdds-client-test";
    private static final long EXPIRATION_MS = 60000; // 1 min

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(SECRET_256_BITS, ISSUER, AUDIENCE, EXPIRATION_MS);

        testUser = User.builder()
                .username("john_farmer")
                .email("john.farmer@eiu.edu.vn")
                .fullName("John Farmer")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        testUser.setId(UUID.randomUUID());
    }

    @Test
    @DisplayName("Should generate valid JWT access token and extract correct claims")
    void testGenerateAndExtractClaims() {
        String token = jwtTokenProvider.generateAccessToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();

        Claims claims = jwtTokenProvider.extractClaims(token);
        assertThat(claims.getSubject()).isEqualTo(testUser.getId().toString());
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getAudience()).contains(AUDIENCE);
        assertThat(jwtTokenProvider.extractUserId(token)).isEqualTo(testUser.getId());
        assertThat(jwtTokenProvider.extractEmail(token)).isEqualTo("john.farmer@eiu.edu.vn");
        assertThat(jwtTokenProvider.extractRole(token)).isEqualTo(Role.ROLE_USER);
    }

    @Test
    @DisplayName("Should reject token when secret or payload is tampered")
    void testTamperedTokenRejection() {
        String token = jwtTokenProvider.generateAccessToken(testUser);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("Should reject expired JWT token")
    void testExpiredTokenRejection() throws InterruptedException {
        // Provider with 1 ms expiration
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(SECRET_256_BITS, ISSUER, AUDIENCE, 1);
        String token = shortLivedProvider.generateAccessToken(testUser);

        Thread.sleep(10); // Wait for expiration

        assertThat(shortLivedProvider.validateToken(token)).isFalse();
    }

    @Test
    @DisplayName("Should reject blank or null token")
    void testNullOrBlankToken() {
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
        assertThat(jwtTokenProvider.validateToken("   ")).isFalse();
    }

    @Test
    @DisplayName("Should fail fast if secret key is less than 256 bits")
    void testWeakSecretKeyRejection() {
        assertThatThrownBy(() -> new JwtTokenProvider("short-key", ISSUER, AUDIENCE, EXPIRATION_MS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 256 bits");
    }

    @Test
    @DisplayName("Should generate cryptographically unique refresh token IDs")
    void testGenerateRefreshTokenId() {
        String token1 = jwtTokenProvider.generateRefreshTokenId();
        String token2 = jwtTokenProvider.generateRefreshTokenId();

        assertThat(token1).isNotBlank();
        assertThat(token2).isNotBlank();
        assertThat(token1).isNotEqualTo(token2);
    }
}
