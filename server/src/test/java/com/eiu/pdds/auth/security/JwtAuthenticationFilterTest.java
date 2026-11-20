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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private UserAuthCache userAuthCache;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    private UUID userId;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        filter = new JwtAuthenticationFilter(jwtTokenProvider, userRepository, userAuthCache);
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
        when(userAuthCache.get(userId)).thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userAuthCache, never()).put(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Cache hit for active user skips PostgreSQL and sets SecurityContext")
    void testDoFilterInternal_CacheHitActiveUser_SkipsDatabase() throws ServletException, IOException {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(jwtTokenProvider.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtTokenProvider.extractClaims("valid.jwt.token")).thenReturn(claims);

        CachedUserAuthState cached = new CachedUserAuthState(
                userId,
                "cached@eiu.edu.vn",
                "cached_farmer",
                Role.ROLE_EXPERT,
                true,
                false
        );
        when(userAuthCache.get(userId)).thenReturn(Optional.of(cached));

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getAuthorities()).extracting("authority").containsExactly("ROLE_EXPERT");
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertThat(principal.id()).isEqualTo(userId);
        assertThat(principal.email()).isEqualTo("cached@eiu.edu.vn");
        assertThat(principal.username()).isEqualTo("cached_farmer");
        assertThat(principal.role()).isEqualTo(Role.ROLE_EXPERT);

        verifyNoInteractions(userRepository);
        verify(userAuthCache, never()).put(any());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Cache miss queries DB and populates UserAuthCache")
    void testDoFilterInternal_CacheMiss_PopulatesCache() throws ServletException, IOException {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(jwtTokenProvider.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtTokenProvider.extractClaims("valid.jwt.token")).thenReturn(claims);
        when(userAuthCache.get(userId)).thenReturn(Optional.empty());

        User activeUser = User.builder()
                .username("fresh_user")
                .email("fresh@eiu.edu.vn")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        activeUser.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));

        filter.doFilterInternal(request, response, filterChain);

        verify(userAuthCache).put(CachedUserAuthState.from(activeUser));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Cached inactive or deleted user clears SecurityContext without DB query")
    void testDoFilterInternal_CachedInactiveOrDeletedUser_ClearsSecurityContextWithoutDb() throws ServletException, IOException {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(jwtTokenProvider.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtTokenProvider.extractClaims("valid.jwt.token")).thenReturn(claims);

        CachedUserAuthState inactive = new CachedUserAuthState(
                userId, "revoked@eiu.edu.vn", "revoked_user", Role.ROLE_ADMIN, false, false
        );
        when(userAuthCache.get(userId)).thenReturn(Optional.of(inactive));

        filter.doFilterInternal(request, response, filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        CachedUserAuthState deleted = new CachedUserAuthState(
                userId, "deleted@eiu.edu.vn", "deleted_user", Role.ROLE_USER, true, true
        );
        when(userAuthCache.get(userId)).thenReturn(Optional.of(deleted));

        filter.doFilterInternal(request, response, filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verifyNoInteractions(userRepository);
        verify(filterChain, times(2)).doFilter(request, response);
    }

    @Test
    @DisplayName("doFilterInternal - Cache eviction forces fresh DB lookup and reflects updated role/status")
    void testDoFilterInternal_CacheEviction_ForcesFreshDbLookup() throws ServletException, IOException {
        InMemoryUserAuthCache realCache = new InMemoryUserAuthCache(180L, Clock.systemUTC());
        JwtAuthenticationFilter filterWithRealCache = new JwtAuthenticationFilter(
                jwtTokenProvider, userRepository, realCache
        );

        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(userId.toString());
        when(jwtTokenProvider.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtTokenProvider.extractClaims("valid.jwt.token")).thenReturn(claims);

        User adminUser = User.builder()
                .username("promoted_user")
                .email("promoted@eiu.edu.vn")
                .role(Role.ROLE_ADMIN)
                .isActive(true)
                .build();
        adminUser.setId(userId);

        User demotedUser = User.builder()
                .username("promoted_user")
                .email("promoted@eiu.edu.vn")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
        demotedUser.setId(userId);

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(adminUser))
                .thenReturn(Optional.of(demotedUser));

        // 1. Cache miss -> queries DB (count = 1), caches ROLE_ADMIN
        filterWithRealCache.doFilterInternal(request, response, filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly("ROLE_ADMIN");

        // 2. Cache hit -> skips DB (count stays 1)
        SecurityContextHolder.clearContext();
        filterWithRealCache.doFilterInternal(request, response, filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly("ROLE_ADMIN");
        verify(userRepository, times(1)).findById(userId);

        // 3. Evict -> forces fresh DB lookup (count = 2), picks up demoted ROLE_USER
        realCache.evict(userId);
        SecurityContextHolder.clearContext();
        filterWithRealCache.doFilterInternal(request, response, filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly("ROLE_USER");
        verify(userRepository, times(2)).findById(userId);
    }
}
