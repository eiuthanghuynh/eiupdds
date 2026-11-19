package com.eiu.pdds.common.ratelimit;

import com.eiu.pdds.auth.security.UserPrincipal;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.user.entity.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Servlet security filter enforcing endpoint-specific distributed rate limits
 * after JWT authentication has populated the security context.
 */
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String DIAGNOSIS_ANALYZE_URI = "/api/v1/diagnoses/analyze";
    private static final String AUTH_URI_PREFIX = "/api/v1/auth/";

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;
    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String uri = request.getRequestURI();
        String key;
        int limit;
        Duration window;

        if (DIAGNOSIS_ANALYZE_URI.equals(uri)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal user) {
                if (user.role() == Role.ROLE_ADMIN || user.role() == Role.ROLE_EXPERT) {
                    filterChain.doFilter(request, response);
                    return;
                }
                key = "pdds:rl:analyze:user:" + user.id();
                limit = properties.getDiagnosisUserLimit();
                window = Duration.ofSeconds(properties.getDiagnosisUserWindowSeconds());
            } else {
                key = "pdds:rl:analyze:ip:" + clientIpResolver.resolve(request);
                limit = properties.getDiagnosisGuestLimit();
                window = Duration.ofSeconds(properties.getDiagnosisGuestWindowSeconds());
            }
        } else if (uri != null && uri.startsWith(AUTH_URI_PREFIX)) {
            key = "pdds:rl:auth:ip:" + clientIpResolver.resolve(request);
            limit = properties.getAuthIpLimit();
            window = Duration.ofSeconds(properties.getAuthIpWindowSeconds());
        } else {
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitResult result = rateLimiter.tryConsume(key, limit, window);

        response.setHeader("X-RateLimit-Limit", String.valueOf(result.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.resetEpochSeconds()));

        if (!result.allowed()) {
            response.setStatus(ErrorCode.RATE_LIMIT_EXCEEDED.getHttpStatus().value());
            response.setHeader("Retry-After", String.valueOf(Math.max(1L, result.retryAfterSeconds())));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", false);
            body.put("code", ErrorCode.RATE_LIMIT_EXCEEDED.getCode());
            body.put("message", ErrorCode.RATE_LIMIT_EXCEEDED.getDefaultMessage());
            body.put("timestamp", Instant.now().toString());
            body.put("path", uri);

            objectMapper.writeValue(response.getOutputStream(), body);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
