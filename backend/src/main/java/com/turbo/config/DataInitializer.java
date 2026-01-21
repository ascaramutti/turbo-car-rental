package com.turbo.config;

import com.turbo.booking.model.Booking;
import com.turbo.booking.model.enums.BookingStatus;
import com.turbo.booking.repository.BookingRepository;
import com.turbo.user.model.Address;
import com.turbo.user.model.Admin;
import com.turbo.user.model.CarOwner;
import com.turbo.user.model.Driver;
import com.turbo.user.model.enums.UserRole;
import com.turbo.user.repository.DriverRepository;
import com.turbo.user.repository.UserRepository;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
            admin.setAddress(new Address("1000 W Georgia St", "Vancouver", "British Columbia", "V6E 3V7", "Canada"));
            userRepository.save(admin);
            System.out.println(">>> Default admin created: admin@turbo.com / admin123");
        }
    }
}
