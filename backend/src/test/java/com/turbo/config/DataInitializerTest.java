package com.turbo.config;

import com.turbo.user.model.Admin;
import com.turbo.user.model.CarOwner;
import com.turbo.user.model.Driver;
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

import java.util.List;

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
        @DisplayName("No existing users - creates admin, driver, and owner")
        void run_noExistingUsers_createsAllThree() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");

            dataInitializer.run();

            verify(userRepository, times(3)).save(userCaptor.capture());
            List<User> savedUsers = userCaptor.getAllValues();

            assertThat(savedUsers.get(0)).isInstanceOf(Admin.class);
            assertThat(savedUsers.get(0).getEmail()).isEqualTo("admin@turbo.com");

            assertThat(savedUsers.get(1)).isInstanceOf(Driver.class);
            assertThat(savedUsers.get(1).getEmail()).isEqualTo("driver@turbo.com");

            assertThat(savedUsers.get(2)).isInstanceOf(CarOwner.class);
            assertThat(savedUsers.get(2).getEmail()).isEqualTo("owner@turbo.com");
        }
    }

    // ── Verifications ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Verifications")
    class Verifications {

        @Test
        @DisplayName("Password encoder is called for each new user")
        void run_noExistingUsers_encodesPasswords() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");

            dataInitializer.run();

            verify(passwordEncoder).encode("admin123");
            verify(passwordEncoder).encode("driver123");
            verify(passwordEncoder).encode("owner123");
        }

        @Test
        @DisplayName("Checks existence for all three emails")
        void run_checksExistenceForAllEmails() {
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            dataInitializer.run();

            verify(userRepository).existsByEmail("admin@turbo.com");
            verify(userRepository).existsByEmail("driver@turbo.com");
            verify(userRepository).existsByEmail("owner@turbo.com");
        }

        @Test
        @DisplayName("Admin is created with emailVerified=true and department set")
        void run_adminHasCorrectFields() {
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");

            dataInitializer.run();

            verify(userRepository, times(3)).save(userCaptor.capture());
            Admin admin = (Admin) userCaptor.getAllValues().get(0);

            assertThat(admin.getEmailVerified()).isTrue();
            assertThat(admin.getDepartment()).isEqualTo("Platform Management");
        }
    }

    // ── Unhappy paths (all users exist) ─────────────────────────────────

    @Nested
    @DisplayName("Unhappy paths")
    class UnhappyPaths {

        @Test
        @DisplayName("All users exist - no save calls")
        void run_allUsersExist_noSaves() {
            when(userRepository.existsByEmail(anyString())).thenReturn(true);

            dataInitializer.run();

            verify(userRepository, never()).save(any());
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("Only admin exists - creates driver and owner only")
        void run_adminExists_createsDriverAndOwner() {
            when(userRepository.existsByEmail("admin@turbo.com")).thenReturn(true);
            when(userRepository.existsByEmail("driver@turbo.com")).thenReturn(false);
            when(userRepository.existsByEmail("owner@turbo.com")).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");

            dataInitializer.run();

            verify(userRepository, times(2)).save(userCaptor.capture());
            List<User> savedUsers = userCaptor.getAllValues();

            assertThat(savedUsers.get(0)).isInstanceOf(Driver.class);
            assertThat(savedUsers.get(1)).isInstanceOf(CarOwner.class);
        }
    }
}
