package com.turbo.config;

import com.turbo.user.model.Admin;
import com.turbo.user.model.CarOwner;
import com.turbo.user.model.Driver;
import com.turbo.user.model.enums.UserRole;
import com.turbo.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@turbo.com")) {
            Admin admin = new Admin();
            admin.setEmail("admin@turbo.com");
            
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFirstName("Turbo");
            admin.setLastName("Admin");
            admin.setRole(UserRole.ADMIN);
            admin.setIsVerified(true);
            admin.setEmailVerified(true);
            admin.setDepartment("Platform Management");
            userRepository.save(admin);
            System.out.println(">>> Default admin created: admin@turbo.com / admin123");
        }

        if (!userRepository.existsByEmail("driver@turbo.com")) {
            Driver driver = new Driver();
            driver.setEmail("driver@turbo.com");
            
            driver.setPassword(passwordEncoder.encode("driver123"));
            driver.setFirstName("John");
            driver.setLastName("Driver");
            driver.setPhoneNumber("+1 604 555 0001");
            driver.setRole(UserRole.DRIVER);
            driver.setIsVerified(false);
            driver.setEmailVerified(true);
            driver.setRating(0.0f);
            driver.setIsWorkEligible(false);
            userRepository.save(driver);
            System.out.println(">>> Test driver created: driver@turbo.com / driver123");
        }

        if (!userRepository.existsByEmail("owner@turbo.com")) {
            CarOwner owner = new CarOwner();
            owner.setEmail("owner@turbo.com");
            
            owner.setPassword(passwordEncoder.encode("owner123"));
            owner.setFirstName("Sarah");
            owner.setLastName("Owner");
            owner.setPhoneNumber("+1 604 555 0002");
            owner.setRole(UserRole.CAR_OWNER);
            owner.setIsVerified(false);
            owner.setEmailVerified(true);
            owner.setRating(0.0f);
            userRepository.save(owner);
            System.out.println(">>> Test owner created: owner@turbo.com / owner123");
        }
    }
}
