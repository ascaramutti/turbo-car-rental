package com.turbo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthFilter")
class JwtAuthFilterTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private UserDetailsService userDetailsService;
    @Mock private FilterChain filterChain;

    @InjectMocks private JwtAuthFilter jwtAuthFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private static final String VALID_JWT = "eyJhbGciOiJIUzI1NiJ9.test.token";
    private static final String EMAIL = "driver@test.com";

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ── Happy path ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy paths")
    class HappyPaths {

        @Test
        @DisplayName("Valid Bearer token - sets authentication in SecurityContext")
        void doFilterInternal_validToken_setsAuthentication() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + VALID_JWT);
            UserDetails userDetails = new User(EMAIL, "password", List.of(new SimpleGrantedAuthority("DRIVER")));

            when(jwtUtil.extractEmail(VALID_JWT)).thenReturn(EMAIL);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
            when(jwtUtil.isTokenValid(VALID_JWT, EMAIL)).thenReturn(true);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo(EMAIL);
            verify(filterChain).doFilter(request, response);
        }
    }

    // ── Verifications ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Verifications")
    class Verifications {

        @Test
        @DisplayName("FilterChain.doFilter is always called")
        void doFilterInternal_alwaysCallsFilterChain() throws ServletException, IOException {
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Valid token - calls extractEmail, loadUserByUsername, and isTokenValid")
        void doFilterInternal_validToken_callsAllDependencies() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + VALID_JWT);
            UserDetails userDetails = new User(EMAIL, "password", List.of(new SimpleGrantedAuthority("DRIVER")));

            when(jwtUtil.extractEmail(VALID_JWT)).thenReturn(EMAIL);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
            when(jwtUtil.isTokenValid(VALID_JWT, EMAIL)).thenReturn(true);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            verify(jwtUtil).extractEmail(VALID_JWT);
            verify(userDetailsService).loadUserByUsername(EMAIL);
            verify(jwtUtil).isTokenValid(VALID_JWT, EMAIL);
        }
    }

    // ── Unhappy paths ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Unhappy paths")
    class UnhappyPaths {

        @Test
        @DisplayName("No Authorization header - skips authentication, calls filterChain")
        void doFilterInternal_noHeader_skipsAuth() throws ServletException, IOException {
            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtUtil, userDetailsService);
        }

        @Test
        @DisplayName("Non-Bearer header - skips authentication, calls filterChain")
        void doFilterInternal_nonBearerHeader_skipsAuth() throws ServletException, IOException {
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(jwtUtil, userDetailsService);
        }

        @Test
        @DisplayName("Invalid token (extractEmail throws) - continues without authentication")
        void doFilterInternal_invalidToken_continuesWithoutAuth() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer invalid.token.here");
            when(jwtUtil.extractEmail("invalid.token.here")).thenThrow(new RuntimeException("Invalid JWT"));

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Token not valid for user - does not set authentication")
        void doFilterInternal_tokenNotValid_doesNotSetAuth() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + VALID_JWT);
            UserDetails userDetails = new User(EMAIL, "password", List.of(new SimpleGrantedAuthority("DRIVER")));

            when(jwtUtil.extractEmail(VALID_JWT)).thenReturn(EMAIL);
            when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
            when(jwtUtil.isTokenValid(VALID_JWT, EMAIL)).thenReturn(false);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Null email extracted - does not attempt user lookup")
        void doFilterInternal_nullEmail_skipsUserLookup() throws ServletException, IOException {
            request.addHeader("Authorization", "Bearer " + VALID_JWT);
            when(jwtUtil.extractEmail(VALID_JWT)).thenReturn(null);

            jwtAuthFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verifyNoInteractions(userDetailsService);
            verify(filterChain).doFilter(request, response);
        }
    }
}
