package com.eiu.pdds.auth.security;

import com.eiu.pdds.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Custom authentication entry point returning standardized 401 JSON responses.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorBody = Map.of(
                "success", false,
                "code", ErrorCode.UNAUTHORIZED.getCode(),
                "message", ErrorCode.UNAUTHORIZED.getDefaultMessage(),
                "timestamp", Instant.now().toString(),
                "path", request.getRequestURI()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
