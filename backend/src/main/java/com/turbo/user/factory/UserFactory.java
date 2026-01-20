package com.turbo.user.factory;

import com.turbo.exception.BusinessException;
import com.turbo.exception.error.AuthErrorCode;
import com.turbo.user.model.CarOwner;
import com.turbo.user.model.Driver;
import com.turbo.user.model.User;
import com.turbo.user.model.enums.UserRole;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {

    public User createByRole(UserRole role) {
        return switch (role) {
            case DRIVER -> buildDriver();
            case CAR_OWNER -> buildCarOwner();
            default -> throw new BusinessException(AuthErrorCode.INVALID_ROLE);
        };
    }

    private Driver buildDriver() {
        Driver driver = new Driver();
        driver.setIsWorkEligible(false);
        return driver;
    }

    private CarOwner buildCarOwner() {
        return new CarOwner();
    }
}
