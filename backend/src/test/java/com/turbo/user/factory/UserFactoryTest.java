package com.turbo.user.factory;

import com.turbo.exception.BusinessException;
import com.turbo.exception.error.AuthErrorCode;
import com.turbo.user.model.CarOwner;
import com.turbo.user.model.Driver;
import com.turbo.user.model.User;
import com.turbo.user.model.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserFactory")
class UserFactoryTest {

    private final UserFactory userFactory = new UserFactory();

    // ── Happy paths ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy paths")
    class HappyPaths {

        @Test
        @DisplayName("DRIVER role - returns Driver with default values")
        void createByRole_driver_returnsDriverWithDefaults() {
            User result = userFactory.createByRole(UserRole.DRIVER);

            assertThat(result).isInstanceOf(Driver.class);
            Driver driver = (Driver) result;
            assertThat(driver.getRating()).isEqualTo(0.0f);
            assertThat(driver.getIsWorkEligible()).isFalse();
        }

        @Test
        @DisplayName("CAR_OWNER role - returns CarOwner with default values")
        void createByRole_carOwner_returnsCarOwnerWithDefaults() {
            User result = userFactory.createByRole(UserRole.CAR_OWNER);

            assertThat(result).isInstanceOf(CarOwner.class);
            CarOwner carOwner = (CarOwner) result;
            assertThat(carOwner.getRating()).isEqualTo(0.0f);
        }
    }

    // ── Unhappy paths ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Unhappy paths")
    class UnhappyPaths {

        @Test
        @DisplayName("ADMIN role - throws BusinessException with AUTH-007")
        void createByRole_admin_throwsInvalidRole() {
            assertThatThrownBy(() -> userFactory.createByRole(UserRole.ADMIN))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(AuthErrorCode.INVALID_ROLE);
        }
    }
}
