package com.turbo.booking.service.impl;

import com.turbo.booking.fixture.BookingFixture;
import com.turbo.booking.model.Booking;
import com.turbo.booking.model.enums.BookingStatus;
import com.turbo.booking.repository.BookingRepository;
import com.turbo.booking.service.LocationMaskService;
import com.turbo.booking.service.command.CancelBookingCommand;
import com.turbo.booking.service.command.CompleteBookingCommand;
import com.turbo.booking.service.command.ConfirmBookingCommand;
import com.turbo.booking.service.command.CreateBookingCommand;
import com.turbo.booking.service.command.GetBookingCommand;
import com.turbo.booking.service.command.RejectBookingCommand;
import com.turbo.booking.service.command.SearchVehiclesCommand;
import com.turbo.booking.repository.BookingPhotoRepository;
import com.turbo.booking.service.command.StartBookingCommand;
import com.turbo.booking.validation.BookingValidationConstraints;
import com.turbo.document.service.FileStorageService;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.BookingErrorCode;
import com.turbo.exception.error.DocumentErrorCode;
import com.turbo.exception.error.VehicleErrorCode;
import com.turbo.user.model.Driver;
import com.turbo.user.model.enums.UserRole;
import com.turbo.user.repository.DriverRepository;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.model.enums.ServiceType;
import com.turbo.vehicle.repository.VehicleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingServiceImpl")
class BookingServiceImplTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private BookingPhotoRepository bookingPhotoRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private DriverRepository driverRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private LocationMaskService locationMaskService;

    @InjectMocks private BookingServiceImpl bookingService;

    // ── Helpers ───────────────────────────────────────────────────────

    private void mockDriverFound(Driver driver) {
        when(driverRepository.findById(driver.getUserId())).thenReturn(Optional.of(driver));
    }

    private void mockVehicleFound(Vehicle vehicle) {
        when(vehicleRepository.findById(vehicle.getVehicleId())).thenReturn(Optional.of(vehicle));
    }

    private void mockBookingFound(Booking booking) {
        when(bookingRepository.findById(booking.getBookingId())).thenReturn(Optional.of(booking));
    }

    private void mockNoVehicleConflict() {
        when(bookingRepository.findConflictingBookings(
                eq(BookingFixture.VEHICLE_ID), anyList(), any(), any()))
                .thenReturn(List.of());
    }

    private void mockNoDriverConflict() {
        when(bookingRepository.findDriverConflictingBookings(
                eq(BookingFixture.DRIVER_ID), anyList(), any(), any()))
                .thenReturn(List.of());
    }

    private void mockBookingSaved(Booking booking) {
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── createBooking ─────────────────────────────────────────────────

    @Nested
    @DisplayName("createBooking()")
    class CreateBooking {

        @Test
        @DisplayName("Happy path - creates PENDING booking and saves it")
        void createBooking_happyPath_returnsSavedBooking() {
            Driver driver = BookingFixture.verifiedDriver();
            Vehicle vehicle = BookingFixture.activeVehicle();

            mockDriverFound(driver);
            mockVehicleFound(vehicle);
            mockNoVehicleConflict();
            mockNoDriverConflict();
            mockBookingSaved(null);

            CreateBookingCommand command = BookingFixture.createBookingCommand();
            Booking result = bookingService.createBooking(command);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(BookingStatus.PENDING);
            assertThat(result.getDriver()).isEqualTo(driver);
            assertThat(result.getVehicle()).isEqualTo(vehicle);
            verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("BOOK-001: driver not verified - throws DRIVER_NOT_VERIFIED")
        void createBooking_driverNotVerified_throwsBook001() {
            Driver driver = BookingFixture.unverifiedDriver();
            mockDriverFound(driver);

            assertThatThrownBy(() -> bookingService.createBooking(BookingFixture.createBookingCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.DRIVER_NOT_VERIFIED);
        }

        @Test
        @DisplayName("BOOK-002: vehicle not approved - throws VEHICLE_NOT_APPROVED")
        void createBooking_vehicleNotApproved_throwsBook002() {
            Driver driver = BookingFixture.verifiedDriver();
            Vehicle vehicle = BookingFixture.pendingVehicle(); // status = PENDING

            mockDriverFound(driver);
            mockVehicleFound(vehicle);

            assertThatThrownBy(() -> bookingService.createBooking(BookingFixture.createBookingCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.VEHICLE_NOT_APPROVED);
        }

        @Test
        @DisplayName("BOOK-003: vehicle not active - throws VEHICLE_NOT_AVAILABLE")
        void createBooking_vehicleNotActive_throwsBook003() {
            Driver driver = BookingFixture.verifiedDriver();
            Vehicle vehicle = BookingFixture.inactiveVehicle(); // approved but isActive=false

            mockDriverFound(driver);
            mockVehicleFound(vehicle);

            assertThatThrownBy(() -> bookingService.createBooking(BookingFixture.createBookingCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.VEHICLE_NOT_AVAILABLE);
        }

        @Test
        @DisplayName("BOOK-004: end time exceeds vehicle availability - throws BOOKING_EXCEEDS_AVAILABILITY")
        void createBooking_endTimeExceedsAvailability_throwsBook004() {
            Driver driver = BookingFixture.verifiedDriver();
            Vehicle vehicle = BookingFixture.activeVehicle();
            // availableUntil is 30 days from now but we'll set it to tomorrow
            vehicle.setAvailableUntil(LocalDateTime.now().plusDays(1));

            mockDriverFound(driver);
            mockVehicleFound(vehicle);

            // command has endTime = tomorrow + 8h, which exceeds availableUntil
            CreateBookingCommand command = BookingFixture.createBookingCommand();
            command.setStartTime(LocalDateTime.now().plusHours(4));
            command.setEndTime(LocalDateTime.now().plusDays(2)); // after availableUntil

            assertThatThrownBy(() -> bookingService.createBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.BOOKING_EXCEEDS_AVAILABILITY);
        }

        @Test
        @DisplayName("BOOK-005: start time not in future - throws START_TIME_NOT_FUTURE")
        void createBooking_startTimeInPast_throwsBook005() {
            Driver driver = BookingFixture.verifiedDriver();
            Vehicle vehicle = BookingFixture.activeVehicle();
            vehicle.setAvailableUntil(LocalDateTime.now().plusDays(30));

            mockDriverFound(driver);
            mockVehicleFound(vehicle);

            CreateBookingCommand command = BookingFixture.createBookingCommand();
            command.setStartTime(LocalDateTime.now().minusHours(1)); // in the past
            command.setEndTime(LocalDateTime.now().plusHours(7));

            assertThatThrownBy(() -> bookingService.createBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.START_TIME_NOT_FUTURE);
        }

        @Test
        @DisplayName("BOOK-006: end time before start time - throws END_TIME_BEFORE_START")
        void createBooking_endTimeBeforeStart_throwsBook006() {
            Driver driver = BookingFixture.verifiedDriver();
            Vehicle vehicle = BookingFixture.activeVehicle();

            mockDriverFound(driver);
            mockVehicleFound(vehicle);

            CreateBookingCommand command = BookingFixture.createBookingCommand();
            command.setStartTime(LocalDateTime.now().plusDays(2));
            command.setEndTime(LocalDateTime.now().plusDays(1)); // before start

            assertThatThrownBy(() -> bookingService.createBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.END_TIME_BEFORE_START);
        }

        @Test
        @DisplayName("BOOK-007: duration less than 4 hours - throws MINIMUM_DURATION_NOT_MET")
        void createBooking_durationTooShort_throwsBook007() {
            Driver driver = BookingFixture.verifiedDriver();
            Vehicle vehicle = BookingFixture.activeVehicle();

            mockDriverFound(driver);
            mockVehicleFound(vehicle);

            CreateBookingCommand command = BookingFixture.createBookingCommand();
            command.setStartTime(LocalDateTime.now().plusDays(1));
            command.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2)); // only 2h

            assertThatThrownBy(() -> bookingService.createBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.MINIMUM_DURATION_NOT_MET);
        }

        @Test
        @DisplayName("BOOK-008: vehicle already booked - throws VEHICLE_ALREADY_BOOKED")
        void createBooking_vehicleConflict_throwsBook008() {
            Driver driver = BookingFixture.verifiedDriver();
            Vehicle vehicle = BookingFixture.activeVehicle();

            mockDriverFound(driver);
            mockVehicleFound(vehicle);

            // conflict exists
            when(bookingRepository.findConflictingBookings(
                    eq(BookingFixture.VEHICLE_ID), anyList(), any(), any()))
                    .thenReturn(List.of(BookingFixture.confirmedBooking()));

            assertThatThrownBy(() -> bookingService.createBooking(BookingFixture.createBookingCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.VEHICLE_ALREADY_BOOKED);
        }

        @Test
        @DisplayName("BOOK-009: driver is the vehicle owner - throws CANNOT_BOOK_OWN_VEHICLE")
        void createBooking_driverIsOwner_throwsBook009() {
            Driver driver = BookingFixture.verifiedDriver();
            Vehicle vehicle = BookingFixture.activeVehicleOwnedByDriver();

            mockDriverFound(driver);
            mockVehicleFound(vehicle);
            mockNoVehicleConflict();

            assertThatThrownBy(() -> bookingService.createBooking(BookingFixture.createBookingCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.CANNOT_BOOK_OWN_VEHICLE);
        }

        @Test
        @DisplayName("BOOK-010: driver has overlapping confirmed booking - throws DRIVER_HAS_OVERLAPPING_BOOKING")
        void createBooking_driverConflict_throwsBook010() {
            Driver driver = BookingFixture.verifiedDriver();
            Vehicle vehicle = BookingFixture.activeVehicle();

            mockDriverFound(driver);
            mockVehicleFound(vehicle);
            mockNoVehicleConflict();

            when(bookingRepository.findDriverConflictingBookings(
                    eq(BookingFixture.DRIVER_ID), anyList(), any(), any()))
                    .thenReturn(List.of(BookingFixture.confirmedBooking()));

            assertThatThrownBy(() -> bookingService.createBooking(BookingFixture.createBookingCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.DRIVER_HAS_OVERLAPPING_BOOKING);
        }

        @Test
        @DisplayName("BOOK-025: duration exceeds 24 hours - throws MAXIMUM_DURATION_EXCEEDED")
        void createBooking_durationTooLong_throwsBook025() {
            Driver driver = BookingFixture.verifiedDriver();
            Vehicle vehicle = BookingFixture.activeVehicle();

            mockDriverFound(driver);
            mockVehicleFound(vehicle);

            CreateBookingCommand command = BookingFixture.createBookingCommand();
            command.setStartTime(LocalDateTime.now().plusDays(1));
            command.setEndTime(LocalDateTime.now().plusDays(1).plusHours(25)); // 25h > 24h max

            assertThatThrownBy(() -> bookingService.createBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.MAXIMUM_DURATION_EXCEEDED);
        }

        @Test
        @DisplayName("VEH-004: vehicle not found - throws VEHICLE_NOT_FOUND")
        void createBooking_vehicleNotFound_throwsVehicleNotFound() {
            Driver driver = BookingFixture.verifiedDriver();
            mockDriverFound(driver);
            when(vehicleRepository.findById(BookingFixture.VEHICLE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.createBooking(BookingFixture.createBookingCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(VehicleErrorCode.VEHICLE_NOT_FOUND);
        }
    }

    // ── confirmBooking ────────────────────────────────────────────────

    @Nested
    @DisplayName("confirmBooking()")
    class ConfirmBooking {

        @Test
        @DisplayName("Happy path - PENDING booking becomes CONFIRMED with pickup location")
        void confirmBooking_happyPath_setsConfirmedStatus() {
            Booking booking = BookingFixture.pendingBooking();
            mockBookingFound(booking);
            mockBookingSaved(booking);
            when(bookingRepository.findPendingOverlapsForDriver(any(), any(), any(), any()))
                    .thenReturn(List.of());

            ConfirmBookingCommand command = BookingFixture.confirmCommand();
            Booking result = bookingService.confirmBooking(command);

            assertThat(result.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
            assertThat(result.getConfirmedAt()).isNotNull();
            assertThat(result.getPickupLocation()).isEqualTo(booking.getVehicle().getGeneralLocation());
            verify(bookingRepository).save(any(Booking.class));
        }

        @Test
        @DisplayName("BOOK-011: booking not found - throws BOOKING_NOT_FOUND")
        void confirmBooking_notFound_throwsBook011() {
            when(bookingRepository.findById(BookingFixture.BOOKING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.confirmBooking(BookingFixture.confirmCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.BOOKING_NOT_FOUND);
        }

        @Test
        @DisplayName("BOOK-012: different owner - throws BOOKING_ACCESS_DENIED")
        void confirmBooking_wrongOwner_throwsBook012() {
            Booking booking = BookingFixture.pendingBooking();
            mockBookingFound(booking);

            ConfirmBookingCommand command = new ConfirmBookingCommand();
            command.setBookingId(BookingFixture.BOOKING_ID);
            command.setOwnerId(999L); // wrong owner

            assertThatThrownBy(() -> bookingService.confirmBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.BOOKING_ACCESS_DENIED);
        }

        @Test
        @DisplayName("BOOK-013: booking not PENDING - throws ONLY_PENDING_CAN_BE_CONFIRMED")
        void confirmBooking_notPending_throwsBook013() {
            Booking booking = BookingFixture.confirmedBooking(); // already CONFIRMED
            mockBookingFound(booking);

            assertThatThrownBy(() -> bookingService.confirmBooking(BookingFixture.confirmCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.ONLY_PENDING_CAN_BE_CONFIRMED);
        }

        @Test
        @DisplayName("BOOK-003: vehicle not active at confirm time - throws VEHICLE_NOT_AVAILABLE")
        void confirmBooking_vehicleInactive_throwsBook003() {
            Booking booking = BookingFixture.pendingBooking();
            booking.getVehicle().setIsActive(false); // deactivated before confirm
            mockBookingFound(booking);

            assertThatThrownBy(() -> bookingService.confirmBooking(BookingFixture.confirmCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.VEHICLE_NOT_AVAILABLE);
        }

        @Test
        @DisplayName("On confirm - overlapping PENDING bookings are auto-rejected")
        void confirmBooking_autoRejectsOverlappingPendingBookings() {
            Booking booking = BookingFixture.pendingBooking();
            mockBookingFound(booking);
            mockBookingSaved(booking);

            Booking overlapping = BookingFixture.pendingBooking();
            overlapping.setBookingId(999L);
            when(bookingRepository.findPendingOverlapsForDriver(any(), any(), any(), any()))
                    .thenReturn(List.of(overlapping));

            bookingService.confirmBooking(BookingFixture.confirmCommand());

            assertThat(overlapping.getStatus()).isEqualTo(BookingStatus.REJECTED);
            assertThat(overlapping.getCancellationReason())
                    .isEqualTo(BookingValidationConstraints.AUTO_REJECT_REASON);
            verify(bookingRepository).saveAll(anyList());
        }
    }

    // ── rejectBooking ─────────────────────────────────────────────────

    @Nested
    @DisplayName("rejectBooking()")
    class RejectBooking {

        @Test
        @DisplayName("Happy path - PENDING booking becomes REJECTED")
        void rejectBooking_happyPath_setsRejectedStatus() {
            Booking booking = BookingFixture.pendingBooking();
            mockBookingFound(booking);
            mockBookingSaved(booking);

            Booking result = bookingService.rejectBooking(BookingFixture.rejectCommand());

            assertThat(result.getStatus()).isEqualTo(BookingStatus.REJECTED);
            assertThat(result.getCancellationReason()).isEqualTo("Vehicle unavailable");
        }

        @Test
        @DisplayName("BOOK-011: booking not found - throws BOOKING_NOT_FOUND")
        void rejectBooking_notFound_throwsBook011() {
            when(bookingRepository.findById(BookingFixture.BOOKING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.rejectBooking(BookingFixture.rejectCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.BOOKING_NOT_FOUND);
        }

        @Test
        @DisplayName("BOOK-012: wrong owner - throws BOOKING_ACCESS_DENIED")
        void rejectBooking_wrongOwner_throwsBook012() {
            Booking booking = BookingFixture.pendingBooking();
            mockBookingFound(booking);

            RejectBookingCommand command = new RejectBookingCommand();
            command.setBookingId(BookingFixture.BOOKING_ID);
            command.setOwnerId(999L);
            command.setReason("Reason");

            assertThatThrownBy(() -> bookingService.rejectBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.BOOKING_ACCESS_DENIED);
        }

        @Test
        @DisplayName("BOOK-014: booking not PENDING - throws ONLY_PENDING_CAN_BE_REJECTED")
        void rejectBooking_notPending_throwsBook014() {
            Booking booking = BookingFixture.confirmedBooking();
            mockBookingFound(booking);

            assertThatThrownBy(() -> bookingService.rejectBooking(BookingFixture.rejectCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.ONLY_PENDING_CAN_BE_REJECTED);
        }

        @Test
        @DisplayName("BOOK-023: missing rejection reason - throws REJECTION_REASON_REQUIRED")
        void rejectBooking_missingReason_throwsBook023() {
            Booking booking = BookingFixture.pendingBooking();
            mockBookingFound(booking);

            RejectBookingCommand command = new RejectBookingCommand();
            command.setBookingId(BookingFixture.BOOKING_ID);
            command.setOwnerId(BookingFixture.OWNER_ID);
            command.setReason("  "); // blank

            assertThatThrownBy(() -> bookingService.rejectBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.REJECTION_REASON_REQUIRED);
        }
    }

    // ── cancelBooking ─────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelBooking()")
    class CancelBooking {

        @Test
        @DisplayName("Happy path (driver, PENDING booking) - becomes CANCELLED by DRIVER")
        void cancelBooking_driverCancelsPending_setsCancelledByDriver() {
            Booking booking = BookingFixture.pendingBooking();
            mockBookingFound(booking);
            mockBookingSaved(booking);

            Booking result = bookingService.cancelBooking(BookingFixture.cancelCommandForDriver());

            assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(result.getCancelledBy()).isEqualTo(BookingValidationConstraints.CANCELLED_BY_DRIVER);
            assertThat(result.getCancellationReason()).isEqualTo("Plans changed");
        }

        @Test
        @DisplayName("Happy path (driver, CONFIRMED booking) - becomes CANCELLED by DRIVER")
        void cancelBooking_driverCancelsConfirmed_setsCancelledByDriver() {
            Booking booking = BookingFixture.confirmedBooking();
            mockBookingFound(booking);
            mockBookingSaved(booking);

            Booking result = bookingService.cancelBooking(BookingFixture.cancelCommandForDriver());

            assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(result.getCancelledBy()).isEqualTo(BookingValidationConstraints.CANCELLED_BY_DRIVER);
        }

        @Test
        @DisplayName("Happy path (owner, CONFIRMED booking) - becomes CANCELLED by OWNER")
        void cancelBooking_ownerCancelsConfirmed_setsCancelledByOwner() {
            Booking booking = BookingFixture.confirmedBooking();
            mockBookingFound(booking);
            mockBookingSaved(booking);

            Booking result = bookingService.cancelBooking(BookingFixture.cancelCommandForOwner());

            assertThat(result.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(result.getCancelledBy()).isEqualTo(BookingValidationConstraints.CANCELLED_BY_OWNER);
        }

        @Test
        @DisplayName("BOOK-011: booking not found - throws BOOKING_NOT_FOUND")
        void cancelBooking_notFound_throwsBook011() {
            when(bookingRepository.findById(BookingFixture.BOOKING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.cancelBooking(BookingFixture.cancelCommandForDriver()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.BOOKING_NOT_FOUND);
        }

        @Test
        @DisplayName("BOOK-012: driver cancels someone else's booking - throws BOOKING_ACCESS_DENIED")
        void cancelBooking_wrongDriver_throwsBook012() {
            Booking booking = BookingFixture.pendingBooking();
            mockBookingFound(booking);

            CancelBookingCommand command = new CancelBookingCommand();
            command.setBookingId(BookingFixture.BOOKING_ID);
            command.setUserId(999L); // wrong driver
            command.setUserRole(UserRole.DRIVER.name());
            command.setReason("Reason");

            assertThatThrownBy(() -> bookingService.cancelBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.BOOKING_ACCESS_DENIED);
        }

        @Test
        @DisplayName("BOOK-015: driver tries to cancel IN_PROGRESS booking - throws INVALID_STATUS_FOR_CANCELLATION")
        void cancelBooking_driverCancelsInProgress_throwsBook015() {
            Booking booking = BookingFixture.inProgressBooking();
            mockBookingFound(booking);

            assertThatThrownBy(() -> bookingService.cancelBooking(BookingFixture.cancelCommandForDriver()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.INVALID_STATUS_FOR_CANCELLATION);
        }

        @Test
        @DisplayName("BOOK-015: owner tries to cancel PENDING booking - throws INVALID_STATUS_FOR_CANCELLATION")
        void cancelBooking_ownerCancelsPending_throwsBook015() {
            Booking booking = BookingFixture.pendingBooking();
            mockBookingFound(booking);

            assertThatThrownBy(() -> bookingService.cancelBooking(BookingFixture.cancelCommandForOwner()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.INVALID_STATUS_FOR_CANCELLATION);
        }

        @Test
        @DisplayName("BOOK-022: missing cancellation reason - throws CANCELLATION_REASON_REQUIRED")
        void cancelBooking_missingReason_throwsBook022() {
            Booking booking = BookingFixture.pendingBooking();
            mockBookingFound(booking);

            CancelBookingCommand command = new CancelBookingCommand();
            command.setBookingId(BookingFixture.BOOKING_ID);
            command.setUserId(BookingFixture.DRIVER_ID);
            command.setUserRole(UserRole.DRIVER.name());
            command.setReason(null);

            assertThatThrownBy(() -> bookingService.cancelBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.CANCELLATION_REASON_REQUIRED);
        }
    }

    // ── startBooking ──────────────────────────────────────────────────

    @Nested
    @DisplayName("startBooking()")
    class StartBooking {

        @Test
        @DisplayName("Happy path - CONFIRMED booking within start window becomes IN_PROGRESS")
        void startBooking_happyPath_setsInProgressStatus() {
            Booking booking = BookingFixture.confirmedBookingReadyToStart();
            mockBookingFound(booking);
            mockBookingSaved(booking);
            when(fileStorageService.store(any(), any(), any())).thenReturn("bookings/200/pickup/pickup.jpg");
            when(bookingPhotoRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            StartBookingCommand command = BookingFixture.startCommand();
            Booking result = bookingService.startBooking(command);

            assertThat(result.getStatus()).isEqualTo(BookingStatus.IN_PROGRESS);
            assertThat(result.getStartedAt()).isNotNull();
            verify(bookingRepository).save(any(Booking.class));
            verify(bookingPhotoRepository).saveAll(any());
        }

        @Test
        @DisplayName("BOOK-011: booking not found - throws BOOKING_NOT_FOUND")
        void startBooking_notFound_throwsBook011() {
            when(bookingRepository.findById(BookingFixture.BOOKING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.startBooking(BookingFixture.startCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.BOOKING_NOT_FOUND);
        }

        @Test
        @DisplayName("BOOK-012: wrong driver - throws BOOKING_ACCESS_DENIED")
        void startBooking_wrongDriver_throwsBook012() {
            Booking booking = BookingFixture.confirmedBookingReadyToStart();
            mockBookingFound(booking);

            StartBookingCommand command = new StartBookingCommand();
            command.setBookingId(BookingFixture.BOOKING_ID);
            command.setDriverId(999L); // wrong driver

            assertThatThrownBy(() -> bookingService.startBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.BOOKING_ACCESS_DENIED);
        }

        @Test
        @DisplayName("BOOK-016: booking not CONFIRMED - throws ONLY_CONFIRMED_CAN_BE_STARTED")
        void startBooking_notConfirmed_throwsBook016() {
            Booking booking = BookingFixture.pendingBooking();
            mockBookingFound(booking);

            assertThatThrownBy(() -> bookingService.startBooking(BookingFixture.startCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.ONLY_CONFIRMED_CAN_BE_STARTED);
        }

        @Test
        @DisplayName("BOOK-017: too early to start (>15 min before startTime) - throws START_TOO_EARLY")
        void startBooking_tooEarly_throwsBook017() {
            Booking booking = BookingFixture.confirmedBooking();
            // startTime is FUTURE_START (tomorrow), so now is more than 15 min before it
            mockBookingFound(booking);

            assertThatThrownBy(() -> bookingService.startBooking(BookingFixture.startCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.START_TOO_EARLY);
        }

        @Test
        @DisplayName("BOOK-018: start window expired (>1h after startTime) - throws START_WINDOW_EXPIRED")
        void startBooking_windowExpired_throwsBook018() {
            // Set startTime 2 hours in the past → window expired
            LocalDateTime pastStart = LocalDateTime.now().minusHours(2);
            Booking booking = BookingFixture.confirmedBooking();
            booking.setStartTime(pastStart);
            booking.setEndTime(pastStart.plusHours(8));
            mockBookingFound(booking);

            assertThatThrownBy(() -> bookingService.startBooking(BookingFixture.startCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.START_WINDOW_EXPIRED);
        }

        @Test
        @DisplayName("BOOK-020: missing pickup photos (null list) - throws PICKUP_PHOTO_REQUIRED")
        void startBooking_missingPhoto_throwsBook020() {
            Booking booking = BookingFixture.confirmedBookingReadyToStart();
            mockBookingFound(booking);

            StartBookingCommand command = new StartBookingCommand();
            command.setBookingId(BookingFixture.BOOKING_ID);
            command.setDriverId(BookingFixture.DRIVER_ID);
            command.setPickupPhotos(null);

            assertThatThrownBy(() -> bookingService.startBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.PICKUP_PHOTO_REQUIRED);
        }

        @Test
        @DisplayName("BOOK-026: too many pickup photos - throws TOO_MANY_PHOTOS")
        void startBooking_tooManyPhotos_throwsBook026() {
            Booking booking = BookingFixture.confirmedBookingReadyToStart();
            mockBookingFound(booking);

            List<org.springframework.web.multipart.MultipartFile> photos = new java.util.ArrayList<>();
            for (int i = 0; i < 11; i++) {
                photos.add(BookingFixture.validPhoto("pickupPhotos", "pickup" + i + ".jpg"));
            }

            StartBookingCommand command = new StartBookingCommand();
            command.setBookingId(BookingFixture.BOOKING_ID);
            command.setDriverId(BookingFixture.DRIVER_ID);
            command.setPickupPhotos(photos);

            assertThatThrownBy(() -> bookingService.startBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.TOO_MANY_PHOTOS);
        }

        @Test
        @DisplayName("DOC-001: invalid photo format - throws INVALID_FILE_FORMAT")
        void startBooking_invalidPhotoFormat_throwsDocError() {
            Booking booking = BookingFixture.confirmedBookingReadyToStart();
            mockBookingFound(booking);

            StartBookingCommand command = new StartBookingCommand();
            command.setBookingId(BookingFixture.BOOKING_ID);
            command.setDriverId(BookingFixture.DRIVER_ID);
            command.setPickupPhotos(List.of(BookingFixture.invalidFormatPhoto("pickupPhotos")));

            assertThatThrownBy(() -> bookingService.startBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.INVALID_FILE_FORMAT);
        }

        @Test
        @DisplayName("DOC-002: oversized photo - throws FILE_TOO_LARGE")
        void startBooking_oversizedPhoto_throwsDocError() {
            Booking booking = BookingFixture.confirmedBookingReadyToStart();
            mockBookingFound(booking);

            StartBookingCommand command = new StartBookingCommand();
            command.setBookingId(BookingFixture.BOOKING_ID);
            command.setDriverId(BookingFixture.DRIVER_ID);
            command.setPickupPhotos(List.of(BookingFixture.oversizedPhoto("pickupPhotos")));

            assertThatThrownBy(() -> bookingService.startBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(DocumentErrorCode.FILE_TOO_LARGE);
        }
    }

    // ── completeBooking ───────────────────────────────────────────────

    @Nested
    @DisplayName("completeBooking()")
    class CompleteBooking {

        @Test
        @DisplayName("Happy path - IN_PROGRESS booking becomes COMPLETED")
        void completeBooking_happyPath_setsCompletedStatus() {
            Booking booking = BookingFixture.inProgressBooking();
            mockBookingFound(booking);
            mockBookingSaved(booking);
            when(fileStorageService.store(any(), any(), any())).thenReturn("bookings/200/return/return.jpg");
            when(bookingPhotoRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            CompleteBookingCommand command = BookingFixture.completeCommand();
            Booking result = bookingService.completeBooking(command);

            assertThat(result.getStatus()).isEqualTo(BookingStatus.COMPLETED);
            assertThat(result.getCompletedAt()).isNotNull();
            verify(bookingRepository).save(any(Booking.class));
            verify(bookingPhotoRepository).saveAll(any());
        }

        @Test
        @DisplayName("BOOK-011: booking not found - throws BOOKING_NOT_FOUND")
        void completeBooking_notFound_throwsBook011() {
            when(bookingRepository.findById(BookingFixture.BOOKING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.completeBooking(BookingFixture.completeCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.BOOKING_NOT_FOUND);
        }

        @Test
        @DisplayName("BOOK-012: wrong driver - throws BOOKING_ACCESS_DENIED")
        void completeBooking_wrongDriver_throwsBook012() {
            Booking booking = BookingFixture.inProgressBooking();
            mockBookingFound(booking);

            CompleteBookingCommand command = new CompleteBookingCommand();
            command.setBookingId(BookingFixture.BOOKING_ID);
            command.setDriverId(999L);

            assertThatThrownBy(() -> bookingService.completeBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.BOOKING_ACCESS_DENIED);
        }

        @Test
        @DisplayName("BOOK-019: booking not IN_PROGRESS - throws ONLY_IN_PROGRESS_CAN_BE_COMPLETED")
        void completeBooking_notInProgress_throwsBook019() {
            Booking booking = BookingFixture.confirmedBooking();
            mockBookingFound(booking);

            assertThatThrownBy(() -> bookingService.completeBooking(BookingFixture.completeCommand()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.ONLY_IN_PROGRESS_CAN_BE_COMPLETED);
        }

        @Test
        @DisplayName("BOOK-021: missing return photos (null list) - throws RETURN_PHOTO_REQUIRED")
        void completeBooking_missingPhoto_throwsBook021() {
            Booking booking = BookingFixture.inProgressBooking();
            mockBookingFound(booking);

            CompleteBookingCommand command = new CompleteBookingCommand();
            command.setBookingId(BookingFixture.BOOKING_ID);
            command.setDriverId(BookingFixture.DRIVER_ID);
            command.setReturnPhotos(null);

            assertThatThrownBy(() -> bookingService.completeBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.RETURN_PHOTO_REQUIRED);
        }

        @Test
        @DisplayName("BOOK-026: too many return photos - throws TOO_MANY_PHOTOS")
        void completeBooking_tooManyPhotos_throwsBook026() {
            Booking booking = BookingFixture.inProgressBooking();
            mockBookingFound(booking);

            List<org.springframework.web.multipart.MultipartFile> photos = new java.util.ArrayList<>();
            for (int i = 0; i < 11; i++) {
                photos.add(BookingFixture.validPhoto("returnPhotos", "return" + i + ".jpg"));
            }

            CompleteBookingCommand command = new CompleteBookingCommand();
            command.setBookingId(BookingFixture.BOOKING_ID);
            command.setDriverId(BookingFixture.DRIVER_ID);
            command.setReturnPhotos(photos);

            assertThatThrownBy(() -> bookingService.completeBooking(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.TOO_MANY_PHOTOS);
        }
    }

    // ── getBookingForUser ─────────────────────────────────────────────

    @Nested
    @DisplayName("getBookingForUser()")
    class GetBooking {

        @Test
        @DisplayName("Happy path (driver) - returns booking")
        void getBookingForUser_driver_returnsBooking() {
            Booking booking = BookingFixture.pendingBooking();
            mockBookingFound(booking);

            Booking result = bookingService.getBookingForUser(BookingFixture.getBookingCommandForDriver());

            assertThat(result.getBookingId()).isEqualTo(BookingFixture.BOOKING_ID);
        }

        @Test
        @DisplayName("Happy path (owner) - returns booking")
        void getBookingForUser_owner_returnsBooking() {
            Booking booking = BookingFixture.pendingBooking();
            mockBookingFound(booking);

            Booking result = bookingService.getBookingForUser(BookingFixture.getBookingCommandForOwner());

            assertThat(result.getBookingId()).isEqualTo(BookingFixture.BOOKING_ID);
        }

        @Test
        @DisplayName("BOOK-011: booking not found - throws BOOKING_NOT_FOUND")
        void getBookingForUser_notFound_throwsBook011() {
            when(bookingRepository.findById(BookingFixture.BOOKING_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> bookingService.getBookingForUser(BookingFixture.getBookingCommandForDriver()))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.BOOKING_NOT_FOUND);
        }

        @Test
        @DisplayName("BOOK-012: driver accesses another driver's booking - throws BOOKING_ACCESS_DENIED")
        void getBookingForUser_wrongDriver_throwsBook012() {
            Booking booking = BookingFixture.pendingBooking();
            mockBookingFound(booking);

            GetBookingCommand command = new GetBookingCommand();
            command.setBookingId(BookingFixture.BOOKING_ID);
            command.setUserId(999L); // wrong driver
            command.setUserRole(UserRole.DRIVER.name());

            assertThatThrownBy(() -> bookingService.getBookingForUser(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(BookingErrorCode.BOOKING_ACCESS_DENIED);
        }
    }

    // ── searchAvailableVehicles ───────────────────────────────────────

    @Nested
    @DisplayName("searchAvailableVehicles()")
    class SearchVehicles {

        @Test
        @DisplayName("No filters - returns list of available vehicles")
        void searchVehicles_noFilters_returnsResults() {
            Driver driver = BookingFixture.verifiedDriver();
            mockDriverFound(driver);

            Vehicle vehicle = BookingFixture.activeVehicle();
            when(vehicleRepository.searchAvailableVehicles(any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of(vehicle));
            // maskCoordinate is called because vehicle has non-null lat/lng
            lenient().when(locationMaskService.maskCoordinate(any(double.class)))
                    .thenAnswer(inv -> {
                        double coord = (double) inv.getArgument(0);
                        return Math.round(coord * 100.0) / 100.0;
                    });

            SearchVehiclesCommand command = BookingFixture.searchVehiclesCommand();
            var results = bookingService.searchAvailableVehicles(command);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getVehicle().getVehicleId()).isEqualTo(BookingFixture.VEHICLE_ID);
        }

        @Test
        @DisplayName("Geo filter - vehicles out of radius are excluded")
        void searchVehicles_geoFilter_excludesOutOfRadius() {
            Driver driver = BookingFixture.verifiedDriver();
            mockDriverFound(driver);

            Vehicle vehicle = BookingFixture.activeVehicle();
            when(vehicleRepository.searchAvailableVehicles(any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of(vehicle));
            when(locationMaskService.isWithinRadius(any(double.class), any(double.class),
                    any(double.class), any(double.class), any(double.class)))
                    .thenReturn(false); // out of radius → vehicle is filtered out

            SearchVehiclesCommand command = BookingFixture.searchVehiclesCommand();
            command.setLatitude(45.5017);
            command.setLongitude(-73.5673);
            command.setRadiusKm(1.0);

            var results = bookingService.searchAvailableVehicles(command);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("No available vehicles - returns empty list")
        void searchVehicles_noVehicles_returnsEmptyList() {
            Driver driver = BookingFixture.verifiedDriver();
            mockDriverFound(driver);
            when(vehicleRepository.searchAvailableVehicles(any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            var results = bookingService.searchAvailableVehicles(BookingFixture.searchVehiclesCommand());

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("Class 5 driver with TAXI_AND_DELIVERY vehicle - warning is set")
        void searchVehicles_class5DriverTaxiVehicle_setsWarning() {
            Driver driver = BookingFixture.class5Driver();
            mockDriverFound(driver);

            Vehicle vehicle = BookingFixture.activeVehicle();
            vehicle.setServiceType(ServiceType.TAXI_AND_DELIVERY);

            when(vehicleRepository.searchAvailableVehicles(any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of(vehicle));
            lenient().when(locationMaskService.maskCoordinate(any(double.class))).thenReturn(45.50);

            SearchVehiclesCommand command = BookingFixture.searchVehiclesCommand();
            command.setDriverId(driver.getUserId());

            var results = bookingService.searchAvailableVehicles(command);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getEffectiveServiceType()).isEqualTo("DELIVERY_ONLY");
            assertThat(results.get(0).getServiceTypeWarning()).isNotNull();
        }
    }

    // ── getOwnerBookings ──────────────────────────────────────────────

    @Nested
    @DisplayName("getOwnerBookings()")
    class GetOwnerBookings {

        @Test
        @DisplayName("No filters - returns all owner bookings")
        void getOwnerBookings_noFilters_returnsAll() {
            Booking booking = BookingFixture.pendingBooking();
            when(bookingRepository.findByVehicleOwnerUserId(BookingFixture.OWNER_ID))
                    .thenReturn(List.of(booking));

            var results = bookingService.getOwnerBookings(BookingFixture.OWNER_ID, null, null);

            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("With status filter only - returns filtered bookings")
        void getOwnerBookings_withStatus_returnsFiltered() {
            Booking booking = BookingFixture.confirmedBooking();
            when(bookingRepository.findByVehicleOwnerUserIdAndStatus(BookingFixture.OWNER_ID, BookingStatus.CONFIRMED))
                    .thenReturn(List.of(booking));

            var results = bookingService.getOwnerBookings(BookingFixture.OWNER_ID, "CONFIRMED", null);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("With vehicleId filter only - returns bookings for that vehicle")
        void getOwnerBookings_withVehicleId_returnsFiltered() {
            Booking booking = BookingFixture.pendingBooking();
            when(bookingRepository.findByVehicleVehicleIdAndVehicleOwnerUserId(
                    BookingFixture.VEHICLE_ID, BookingFixture.OWNER_ID))
                    .thenReturn(List.of(booking));

            var results = bookingService.getOwnerBookings(BookingFixture.OWNER_ID, null, BookingFixture.VEHICLE_ID);

            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("With both vehicleId and status filters - returns narrowly filtered bookings")
        void getOwnerBookings_withVehicleAndStatus_returnsFiltered() {
            Booking booking = BookingFixture.confirmedBooking();
            when(bookingRepository.findByVehicleVehicleIdAndVehicleOwnerUserIdAndStatus(
                    BookingFixture.VEHICLE_ID, BookingFixture.OWNER_ID, BookingStatus.CONFIRMED))
                    .thenReturn(List.of(booking));

            var results = bookingService.getOwnerBookings(
                    BookingFixture.OWNER_ID, "CONFIRMED", BookingFixture.VEHICLE_ID);

            assertThat(results).hasSize(1);
        }
    }

    // ── getAllBookings (admin) ─────────────────────────────────────────

    @Nested
    @DisplayName("getAllBookings()")
    class GetAllBookings {

        @Test
        @DisplayName("No filters - delegates to findByFilters with null params")
        void getAllBookings_noFilters_returnsAll() {
            Booking booking = BookingFixture.pendingBooking();
            when(bookingRepository.findByFilters(null, null, null))
                    .thenReturn(List.of(booking));

            var results = bookingService.getAllBookings(null, null, null);

            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("With status filter - delegates to findByFilters with parsed status")
        void getAllBookings_withStatus_parsesAndFilters() {
            Booking booking = BookingFixture.confirmedBooking();
            when(bookingRepository.findByFilters(BookingStatus.CONFIRMED, null, null))
                    .thenReturn(List.of(booking));

            var results = bookingService.getAllBookings("CONFIRMED", null, null);

            assertThat(results).hasSize(1);
        }
    }

    // ── autoCancelExpiredPendingBookings ──────────────────────────────

    @Nested
    @DisplayName("autoCancelExpiredPendingBookings() [@Scheduled]")
    class AutoCancelExpiredPendingBookings {

        @Test
        @DisplayName("Cancels all PENDING bookings past their startTime")
        void autoCancelExpiredPendingBookings_cancelsExpiredBookings() {
            Booking expired = BookingFixture.pendingBooking();
            when(bookingRepository.findPendingBookingsPastStartTime(any(LocalDateTime.class)))
                    .thenReturn(List.of(expired));

            bookingService.autoCancelExpiredPendingBookings();

            assertThat(expired.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(expired.getCancelledBy()).isEqualTo(BookingValidationConstraints.CANCELLED_BY_SYSTEM);
            assertThat(expired.getCancellationReason())
                    .isEqualTo(BookingValidationConstraints.AUTO_CANCEL_REASON);
            verify(bookingRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("No expired bookings - does not call saveAll")
        void autoCancelExpiredPendingBookings_noExpired_doesNotSave() {
            when(bookingRepository.findPendingBookingsPastStartTime(any()))
                    .thenReturn(List.of());

            bookingService.autoCancelExpiredPendingBookings();

            verify(bookingRepository, never()).saveAll(anyList());
        }
    }

    // ── autoCompleteOverdueInProgressBookings ─────────────────────────

    @Nested
    @DisplayName("autoCompleteOverdueInProgressBookings() [@Scheduled]")
    class AutoCompleteOverdueInProgressBookings {

        @Test
        @DisplayName("Completes all IN_PROGRESS bookings past grace period")
        void autoCompleteOverdueBookings_completesOverdueBookings() {
            Booking overdue = BookingFixture.inProgressBooking();
            when(bookingRepository.findInProgressBookingsPastGracePeriod(any(LocalDateTime.class)))
                    .thenReturn(List.of(overdue));

            bookingService.autoCompleteOverdueInProgressBookings();

            assertThat(overdue.getStatus()).isEqualTo(BookingStatus.COMPLETED);
            assertThat(overdue.getCompletedAt()).isNotNull();
            verify(bookingRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("No overdue bookings - does not call saveAll")
        void autoCompleteOverdueBookings_noOverdue_doesNotSave() {
            when(bookingRepository.findInProgressBookingsPastGracePeriod(any()))
                    .thenReturn(List.of());

            bookingService.autoCompleteOverdueInProgressBookings();

            verify(bookingRepository, never()).saveAll(anyList());
        }
    }
}
