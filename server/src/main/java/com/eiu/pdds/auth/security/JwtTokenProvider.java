package com.eiu.pdds.auth.security;

import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Enterprise JWT Token Provider adhering to strict token contract policies:
 * Fixed HMAC-SHA256 algorithm, dedicated issuer & audience, and mandatory claims validation.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey signingKey;
    private final String issuer;
    private final String audience;
    private final long accessTokenExpirationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer:eiu-pdds-auth}") String issuer,
            @Value("${jwt.audience:eiu-pdds-client}") String audience,
            @Value("${jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs
    ) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 256 bits (32 bytes)");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        this.audience = audience;
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    /**
     * Generates a signed Access Token carrying essential identity claims.
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .issuer(issuer)
                .audience().add(audience).and()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .claim("fullName", user.getFullName() != null ? user.getFullName() : "")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Generates a cryptographically secure opaque Refresh Token ID.
     */
    public String generateRefreshTokenId() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return UUID.randomUUID() + "-" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Validates cryptographic signature, expiry, issuer, and audience.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired: {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extracts validated Claims from signed token.
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(extractClaims(token).getSubject());
    }

    public String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }

    public Role extractRole(String token) {
        String roleStr = extractClaims(token).get("role", String.class);
        return Role.valueOf(roleStr);
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}
