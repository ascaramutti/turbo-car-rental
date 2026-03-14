package com.turbo.user.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User - onCreate()")
class UserTest {

    @Nested
    @DisplayName("@PrePersist callback")
    class OnCreate {

        @Test
        @DisplayName("Sets createdAt to current timestamp")
        void onCreate_setsCreatedAt() {
            User user = new User();

            user.onCreate();

            assertThat(user.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Null isVerified defaults to false")
        void onCreate_nullIsVerified_defaultsToFalse() {
            User user = new User();
            user.setIsVerified(null);

            user.onCreate();

            assertThat(user.getIsVerified()).isFalse();
        }

        @Test
        @DisplayName("Null emailVerified defaults to false")
        void onCreate_nullEmailVerified_defaultsToFalse() {
            User user = new User();
            user.setEmailVerified(null);

            user.onCreate();

            assertThat(user.getEmailVerified()).isFalse();
        }

        @Test
        @DisplayName("Existing isVerified=true is preserved")
        void onCreate_existingIsVerified_preserved() {
            User user = new User();
            user.setIsVerified(true);

            user.onCreate();

            assertThat(user.getIsVerified()).isTrue();
        }

        @Test
        @DisplayName("Existing emailVerified=true is preserved")
        void onCreate_existingEmailVerified_preserved() {
            User user = new User();
            user.setEmailVerified(true);

            user.onCreate();

            assertThat(user.getEmailVerified()).isTrue();
        }
    }
}
