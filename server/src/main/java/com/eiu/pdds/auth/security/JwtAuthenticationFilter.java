package com.eiu.pdds.auth.security;

import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Filter intercepting incoming requests to authenticate Bearer JWT Access Tokens
 * and synchronize live user role and active status from persistence.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final UserAuthCache userAuthCache;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractBearerToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            try {
                Claims claims = jwtTokenProvider.extractClaims(token);
                UUID userId = UUID.fromString(claims.getSubject());

                Optional<CachedUserAuthState> userState = resolveUserAuthState(userId);
                if (userState.isEmpty() || !userState.get().isUsable()) {
                    log.debug("Rejected JWT for inactive, deleted, or missing user: id={}", userId);
                    SecurityContextHolder.clearContext();
                } else {
                    authenticate(userState.get(), request);
                }
            } catch (Exception e) {
                log.warn("Failed to set security context from JWT token: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private Optional<CachedUserAuthState> resolveUserAuthState(UUID userId) {
        Optional<CachedUserAuthState> cached = userAuthCache.get(userId);
        if (cached.isPresent()) {
            return cached;
        }

        Optional<CachedUserAuthState> fromDatabase = userRepository.findById(userId)
                .map(CachedUserAuthState::from);
        fromDatabase.ifPresent(userAuthCache::put);
        return fromDatabase;
    }

    private void authenticate(CachedUserAuthState state, HttpServletRequest request) {
        Role liveRole = state.role();
        UserPrincipal principal = new UserPrincipal(
                state.id(),
                state.email(),
                state.username(),
                liveRole
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority(liveRole.name()))
        );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
