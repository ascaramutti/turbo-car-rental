package com.turbo.auth.service;

import com.turbo.auth.fixture.AuthFixture;
import com.turbo.user.model.User;
import com.turbo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService")
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks private CustomUserDetailsService customUserDetailsService;

    // ── Happy path ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy paths")
    class HappyPaths {

        @Test
        @DisplayName("Existing email - returns UserDetails with correct email, password, and role")
        void loadUserByUsername_existingEmail_returnsUserDetails() {
            User user = AuthFixture.verifiedUser();
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            UserDetails result = customUserDetailsService.loadUserByUsername(user.getEmail());

            assertThat(result.getUsername()).isEqualTo(user.getEmail());
            assertThat(result.getPassword()).isEqualTo(user.getPassword());
            assertThat(result.getAuthorities()).hasSize(1);
            assertThat(result.getAuthorities().iterator().next().getAuthority()).isEqualTo("DRIVER");
        }
    }

    // ── Verifications ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Verifications")
    class Verifications {

        @Test
        @DisplayName("Repository is called with the provided email")
        void loadUserByUsername_callsRepositoryWithEmail() {
            User user = AuthFixture.verifiedUser();
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

            customUserDetailsService.loadUserByUsername(user.getEmail());

            verify(userRepository).findByEmail(user.getEmail());
        }
    }

    // ── Unhappy paths ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Unhappy paths")
    class UnhappyPaths {

        @Test
        @DisplayName("Non-existing email - throws UsernameNotFoundException")
        void loadUserByUsername_nonExistingEmail_throwsUsernameNotFoundException() {
            String email = "unknown@test.com";
            when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername(email))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage(CustomUserDetailsService.USER_NOT_FOUND_MSG + email);
        }
    }
}
