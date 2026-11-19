package com.eiu.pdds.auth.security;

import com.eiu.pdds.user.entity.Role;
import com.eiu.pdds.user.entity.User;
import com.eiu.pdds.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JwtAuthenticationFilter} verifying live DB role & active-status synchronization.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private UUID userId;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        userId = UUID.randomUUID();
        request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid.jwt.token");
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("doFilterInternal - Uses live DB role instead of stale role claim in JWT")
    void testDoFilterInternal_UsesLiveRoleFromDatabase() throws ServletException, IOException {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(jwtTokenProvider.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtTokenProvider.extractClaims("valid.jwt.token")).thenReturn(claims);

        User demotedUser = User.builder()
                .username("demoted_admin")
                .email("demoted@eiu.edu.vn")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        demotedUser.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(demotedUser));

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertThat(principal.role()).isEqualTo(Role.ROLE_USER);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Clears SecurityContext when user is suspended (isActive=false)")
    void testDoFilterInternal_SuspendedUser_ClearsSecurityContext() throws ServletException, IOException {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(jwtTokenProvider.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtTokenProvider.extractClaims("valid.jwt.token")).thenReturn(claims);

        User suspendedUser = User.builder()
                .username("suspended_user")
                .email("suspended@eiu.edu.vn")
                .role(Role.ROLE_ADMIN)
                .isActive(false)
                .build();
        suspendedUser.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(suspendedUser));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Clears SecurityContext when user is soft-deleted")
    void testDoFilterInternal_DeletedUser_ClearsSecurityContext() throws ServletException, IOException {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(jwtTokenProvider.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtTokenProvider.extractClaims("valid.jwt.token")).thenReturn(claims);

        User deletedUser = User.builder()
                .username("deleted_user")
                .email("deleted@eiu.edu.vn")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        deletedUser.setId(userId);
        deletedUser.setDeletedAt(Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(deletedUser));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Clears SecurityContext when user does not exist in DB")
    void testDoFilterInternal_MissingUser_ClearsSecurityContext() throws ServletException, IOException {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(jwtTokenProvider.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtTokenProvider.extractClaims("valid.jwt.token")).thenReturn(claims);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
