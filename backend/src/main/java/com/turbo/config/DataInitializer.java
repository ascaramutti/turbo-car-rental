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
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final BookingRepository bookingRepository;

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
            driver.setAddress(new Address("456 Granville St", "Vancouver", "British Columbia", "V6C 1T2", "Canada"));
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
            owner.setAddress(new Address("789 Robson St", "Vancouver", "British Columbia", "V6Z 3B7", "Canada"));
            userRepository.save(owner);
            System.out.println(">>> Test owner created: owner@turbo.com / owner123");
        }

        seedSampleBookings();
    }

    /** Seeds three sample bookings if none exist and the prerequisite vehicle is present. */
    private void seedSampleBookings() {
        if (bookingRepository.count() > 0) {
            return;
        }

        userRepository.findByEmail("driver@turbo.com").ifPresent(driverUser -> {
            List<Vehicle> ownerVehicles = vehicleRepository.findAll();
            if (ownerVehicles.isEmpty()) {
                return;
            }

            Vehicle vehicle = ownerVehicles.get(0);

            driverRepository.findById(driverUser.getUserId()).ifPresent(driver -> {
                LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
                LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
                LocalDateTime dayAfterTomorrow = LocalDateTime.now().plusDays(2);

                Booking completedBooking = buildSeedBooking(driver, vehicle,
                        BookingStatus.COMPLETED,
                        yesterday.withHour(8).withMinute(0).withSecond(0),
                        yesterday.withHour(16).withMinute(0).withSecond(0));
                completedBooking.setCompletedAt(yesterday.withHour(16).withMinute(5).withSecond(0));
                completedBooking.setConfirmedAt(yesterday.minusDays(1).withHour(10).withMinute(0).withSecond(0));
                completedBooking.setPickupLocation(vehicle.getGeneralLocation());
                completedBooking.setPickupLatitude(vehicle.getLatitude());
                completedBooking.setPickupLongitude(vehicle.getLongitude());

                Booking confirmedBooking = buildSeedBooking(driver, vehicle,
                        BookingStatus.CONFIRMED,
                        tomorrow.withHour(8).withMinute(0).withSecond(0),
                        tomorrow.withHour(16).withMinute(0).withSecond(0));
                confirmedBooking.setConfirmedAt(LocalDateTime.now().minusHours(1));
                confirmedBooking.setPickupLocation(vehicle.getGeneralLocation());
                confirmedBooking.setPickupLatitude(vehicle.getLatitude());
                confirmedBooking.setPickupLongitude(vehicle.getLongitude());

                Booking pendingBooking = buildSeedBooking(driver, vehicle,
                        BookingStatus.PENDING,
                        dayAfterTomorrow.withHour(8).withMinute(0).withSecond(0),
                        dayAfterTomorrow.withHour(12).withMinute(0).withSecond(0));

                bookingRepository.saveAll(List.of(completedBooking, confirmedBooking, pendingBooking));
                System.out.println(">>> Sample bookings seeded (COMPLETED, CONFIRMED, PENDING)");
            });
        });
    }

    /** Constructs a seed Booking with computed totalHours and totalPrice. */
    private Booking buildSeedBooking(Driver driver, Vehicle vehicle,
                                     BookingStatus status,
                                     LocalDateTime startTime, LocalDateTime endTime) {
        long hours = java.time.Duration.between(startTime, endTime).toHours();
        BigDecimal hourlyRate = vehicle.getHourlyRate() != null
                ? vehicle.getHourlyRate() : BigDecimal.valueOf(20);
        BigDecimal totalPrice = hourlyRate.multiply(BigDecimal.valueOf(hours));

        Booking booking = new Booking();
        booking.setDriver(driver);
        booking.setVehicle(vehicle);
        booking.setStatus(status);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setTotalHours((int) hours);
        booking.setTotalPrice(totalPrice);
        return booking;
    }
}
