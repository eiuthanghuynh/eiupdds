package com.eiu.pdds.auth.security;

import com.eiu.pdds.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Custom access denied handler returning standardized 403 Forbidden JSON responses.
 */
@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorBody = Map.of(
                "success", false,
                "code", ErrorCode.ACCESS_DENIED.getCode(),
                "message", ErrorCode.ACCESS_DENIED.getDefaultMessage(),
                "timestamp", Instant.now().toString(),
                "path", request.getRequestURI()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorBody));
    }
}
