package com.turbo.config;

import com.turbo.user.model.Admin;
import com.turbo.user.model.User;
import com.turbo.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataInitializer")
class DataInitializerTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private DataInitializer dataInitializer;

    @Captor private ArgumentCaptor<User> userCaptor;

    // ── Happy path ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy paths")
    class HappyPaths {

        @Test
        @DisplayName("Admin does not exist - creates admin user")
        void run_adminNotExists_createsAdmin() {
            when(userRepository.existsByEmail("admin@turbo.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");

            dataInitializer.run();

            verify(userRepository, times(1)).save(userCaptor.capture());
            User saved = userCaptor.getValue();

            assertThat(saved).isInstanceOf(Admin.class);
            assertThat(saved.getEmail()).isEqualTo("admin@turbo.com");
        }
    }

    // ── Verifications ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Verifications")
    class Verifications {

        @Test
        @DisplayName("Password encoder is called once for admin")
        void run_adminNotExists_encodesAdminPassword() {
            when(userRepository.existsByEmail("admin@turbo.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");

            dataInitializer.run();

            verify(passwordEncoder).encode("admin123");
        }

        @Test
        @DisplayName("Checks existence only for admin email")
        void run_checksExistenceForAdminEmail() {
            when(userRepository.existsByEmail("admin@turbo.com")).thenReturn(true);

            dataInitializer.run();

            verify(userRepository).existsByEmail("admin@turbo.com");
        }

        @Test
        @DisplayName("Admin is created with emailVerified=true and department set")
        void run_adminHasCorrectFields() {
            when(userRepository.existsByEmail("admin@turbo.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");

            dataInitializer.run();

            verify(userRepository).save(userCaptor.capture());
            Admin admin = (Admin) userCaptor.getValue();

            assertThat(admin.getEmailVerified()).isTrue();
            assertThat(admin.getDepartment()).isEqualTo("Platform Management");
        }
    }

    // ── Unhappy paths ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Unhappy paths")
    class UnhappyPaths {

        @Test
        @DisplayName("Admin already exists - no save calls")
        void run_adminExists_noSaves() {
            when(userRepository.existsByEmail("admin@turbo.com")).thenReturn(true);

            dataInitializer.run();

            verify(userRepository, never()).save(any());
            verifyNoInteractions(passwordEncoder);
        }
    }
}
