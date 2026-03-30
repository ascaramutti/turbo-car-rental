package com.turbo.booking.service.impl;

import com.turbo.booking.model.Booking;
import com.turbo.booking.model.BookingPhoto;
import com.turbo.booking.model.enums.BookingStatus;
import com.turbo.booking.repository.BookingPhotoRepository;
import com.turbo.booking.repository.BookingRepository;
import com.turbo.booking.service.BookingService;
import com.turbo.booking.service.LocationMaskService;
import com.turbo.booking.service.mapper.BookingServiceMapper;
import com.turbo.booking.service.result.DriverHoursSummary;
import com.turbo.booking.service.result.LocationResult;
import com.turbo.booking.service.result.OwnerDashboardStats;
import com.turbo.booking.service.result.VehicleSearchResult;
import com.turbo.booking.service.command.CancelBookingCommand;
import com.turbo.booking.service.command.CompleteBookingCommand;
import com.turbo.booking.service.command.ConfirmBookingCommand;
import com.turbo.booking.service.command.CreateBookingCommand;
import com.turbo.booking.service.command.GetBookingCommand;
import com.turbo.booking.service.command.RejectBookingCommand;
import com.turbo.booking.service.command.SearchVehiclesCommand;
import com.turbo.booking.service.command.StartBookingCommand;
import com.turbo.booking.validation.BookingValidationConstraints;
import com.turbo.document.service.FileStorageService;
import com.turbo.exception.BusinessException;
import com.turbo.exception.error.BookingErrorCode;
import com.turbo.exception.error.DocumentErrorCode;
import com.turbo.exception.error.VehicleErrorCode;
import com.turbo.user.model.CarOwner;
import com.turbo.user.model.Driver;
import com.turbo.user.model.enums.UserRole;
import com.turbo.user.repository.CarOwnerRepository;
import com.turbo.user.repository.DriverRepository;
import com.turbo.vehicle.model.Vehicle;
import com.turbo.vehicle.model.enums.FuelType;
import com.turbo.vehicle.model.enums.ServiceType;
import com.turbo.vehicle.model.enums.VehicleCategory;
import com.turbo.vehicle.model.enums.VehicleStatus;
import com.turbo.vehicle.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {

    private static final List<BookingStatus> CONFLICT_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS);

    private final BookingRepository bookingRepository;
    private final BookingPhotoRepository bookingPhotoRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final CarOwnerRepository carOwnerRepository;
    private final FileStorageService fileStorageService;
    private final LocationMaskService locationMaskService;
    private final BookingServiceMapper serviceMapper;

    // ── Driver: dashboard ────────────────────────────────────────────

    @Override
    public DriverHoursSummary getDriverHoursSummary(Long driverId) {
        LocalDateTime weekStart = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime weekEnd = weekStart.plusWeeks(1);
        List<BookingStatus> countedStatuses =
                List.of(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS, BookingStatus.COMPLETED);
        int hoursUsed = bookingRepository.sumHoursForDriverInWeek(driverId, countedStatuses, weekStart, weekEnd);
        int max = BookingValidationConstraints.MAX_SHIFT_HOURS;
        return serviceMapper.toDriverHoursSummary(hoursUsed, max, Math.max(0, max - hoursUsed));
    }

    // ── Owner: dashboard ─────────────────────────────────────────────

    @Override
    public OwnerDashboardStats getOwnerDashboardStats(Long ownerId) {
        long activeVehicles = vehicleRepository.countByOwnerUserIdAndIsActiveTrue(ownerId);
        BigDecimal totalEarnings = bookingRepository.sumEarningsForOwner(ownerId, BookingStatus.COMPLETED);
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1);
        BigDecimal monthEarnings = bookingRepository.sumEarningsForOwnerInMonth(
                ownerId, BookingStatus.COMPLETED, monthStart, monthEnd);
        long completedCount = bookingRepository.countByVehicleOwnerAndStatus(ownerId, BookingStatus.COMPLETED);
        Float rating = carOwnerRepository.findById(ownerId)
                .map(CarOwner::getRating)
                .orElse(null);
        return serviceMapper.toOwnerDashboardStats(
                (int) activeVehicles, totalEarnings, monthEarnings, (int) completedCount, rating);
    }

    // ── Driver: search ───────────────────────────────────────────────

    @Override
    public List<VehicleSearchResult> searchAvailableVehicles(SearchVehiclesCommand command) {
        Driver driver = findDriverById(command.getDriverId());

        VehicleCategory category = parseOptionalCategory(command.getCategory());
        ServiceType serviceType = parseOptionalServiceType(command.getServiceType());
        FuelType fuelType = parseOptionalFuelType(command.getFuelType());

        List<Vehicle> candidates = vehicleRepository.searchAvailableVehicles(
                LocalDateTime.now(), category, serviceType, fuelType,
                command.getMinPrice(), command.getMaxPrice());

        return candidates.stream()
                .filter(v -> !hasVehicleConflict(v.getVehicleId(), command.getStartTime(), command.getEndTime()))
                .filter(v -> isAvailableUntilEndTime(v, command.getEndTime()))
                .filter(v -> isWithinSearchRadius(v, command))
                .map(v -> buildVehicleSearchResponse(v, driver))
                .toList();
    }

    @Override
    public VehicleSearchResult getVehicleDetail(Long vehicleId, Long driverId) {
        Vehicle vehicle = findVehicleById(vehicleId);
        Driver driver = findDriverById(driverId);
        return buildVehicleBookingDetailResponse(vehicle, driver);
    }

    // ── Driver: booking lifecycle ────────────────────────────────────

    @Override
    @Transactional
    public Booking createBooking(CreateBookingCommand command) {
        Driver driver = findDriverById(command.getDriverId());
        validateDriverIsVerified(driver);

        Vehicle vehicle = findVehicleById(command.getVehicleId());
        validateVehicleApproved(vehicle);
        validateVehicleActive(vehicle);
        validateEndTimeWithinAvailability(command.getEndTime(), vehicle);
        validateStartTimeInFuture(command.getStartTime());
        validateEndTimeAfterStart(command.getStartTime(), command.getEndTime());
        validateMinShiftDuration(command.getStartTime(), command.getEndTime());
        validateMaxShiftDuration(command.getStartTime(), command.getEndTime());
        validateNoVehicleConflict(vehicle.getVehicleId(), command.getStartTime(), command.getEndTime());
        validateDriverNotOwner(vehicle, command.getDriverId());
        validateNoDriverConflict(command.getDriverId(), command.getStartTime(), command.getEndTime());

        long hours = Duration.between(command.getStartTime(), command.getEndTime()).toHours();
        BigDecimal totalPrice = vehicle.getHourlyRate().multiply(BigDecimal.valueOf(hours));
        Booking booking = serviceMapper.toBooking(command, driver, vehicle, (int) hours, totalPrice);
        return bookingRepository.save(booking);
    }

    @Override
    public List<Booking> getDriverBookings(Long driverId, String status) {
        if (status != null && !status.isBlank()) {
            BookingStatus bookingStatus = parseBookingStatus(status);
            return bookingRepository.findByDriverUserIdAndStatus(driverId, bookingStatus);
        }
        return bookingRepository.findByDriverUserId(driverId);
    }

    @Override
    public Booking getBookingForUser(GetBookingCommand command) {
        Booking booking = findBookingById(command.getBookingId());
        validateBookingAccess(booking, command.getUserId(), command.getUserRole());
        return booking;
    }

    @Override
    @Transactional
    public Booking cancelBooking(CancelBookingCommand command) {
        Booking booking = findBookingById(command.getBookingId());
        validateBookingAccess(booking, command.getUserId(), command.getUserRole());
        validateCancellationStatusAllowed(booking, command.getUserRole());
        validateReasonNotBlank(command.getReason(), BookingErrorCode.CANCELLATION_REASON_REQUIRED);

        String cancelledBy = resolveCancelledByFromRole(command.getUserRole());
        applyCancellation(booking, command.getReason(), cancelledBy);
        return bookingRepository.save(booking);
    }

    @Override
    @Transactional
    public Booking startBooking(StartBookingCommand command) {
        Booking booking = findBookingById(command.getBookingId());
        validateBookingBelongsToDriver(booking, command.getDriverId());
        validateStatusEquals(booking, BookingStatus.CONFIRMED, BookingErrorCode.ONLY_CONFIRMED_CAN_BE_STARTED);
        validateStartWindowNotTooEarly(booking);
        validateStartWindowNotExpired(booking);

        List<MultipartFile> photos = command.getPickupPhotos();
        validatePhotosPresent(photos, BookingErrorCode.PICKUP_PHOTO_REQUIRED);
        validatePhotoCount(photos);

        List<BookingPhoto> photoEntities = new ArrayList<>();
        for (MultipartFile photo : photos) {
            validatePhotoFormat(photo);
            validatePhotoSize(photo);
            BookingPhoto entity = storeBookingPhoto(photo, booking,
                    BookingValidationConstraints.PICKUP_PHOTO_TYPE,
                    BookingValidationConstraints.PHOTO_TYPE_PICKUP);
            photoEntities.add(entity);
        }

        booking.setStatus(BookingStatus.IN_PROGRESS);
        booking.setStartedAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);
        bookingPhotoRepository.saveAll(photoEntities);
        return saved;
    }

    @Override
    @Transactional
    public Booking completeBooking(CompleteBookingCommand command) {
        Booking booking = findBookingById(command.getBookingId());
        validateBookingBelongsToDriver(booking, command.getDriverId());
        validateStatusEquals(booking, BookingStatus.IN_PROGRESS, BookingErrorCode.ONLY_IN_PROGRESS_CAN_BE_COMPLETED);

        List<MultipartFile> photos = command.getReturnPhotos();
        validatePhotosPresent(photos, BookingErrorCode.RETURN_PHOTO_REQUIRED);
        validatePhotoCount(photos);

        List<BookingPhoto> photoEntities = new ArrayList<>();
        for (MultipartFile photo : photos) {
            validatePhotoFormat(photo);
            validatePhotoSize(photo);
            BookingPhoto entity = storeBookingPhoto(photo, booking,
                    BookingValidationConstraints.RETURN_PHOTO_TYPE,
                    BookingValidationConstraints.PHOTO_TYPE_RETURN);
            photoEntities.add(entity);
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);
        bookingPhotoRepository.saveAll(photoEntities);
        return saved;
    }

    @Override
    public LocationResult getVehicleLocation(GetBookingCommand command) {
        Booking booking = findBookingById(command.getBookingId());
        validateBookingBelongsToDriver(booking, command.getUserId());
        validateBookingIsConfirmedOrLater(booking);
        return locationMaskService.buildLocationResponse(booking);
    }

    // ── Owner operations ─────────────────────────────────────────────

    @Override
    public List<Booking> getOwnerBookings(Long ownerId, String status, Long vehicleId) {
        if (vehicleId != null && status != null && !status.isBlank()) {
            BookingStatus bookingStatus = parseBookingStatus(status);
            return bookingRepository.findByVehicleVehicleIdAndVehicleOwnerUserIdAndStatus(
                    vehicleId, ownerId, bookingStatus);
        }
        if (vehicleId != null) {
            return bookingRepository.findByVehicleVehicleIdAndVehicleOwnerUserId(vehicleId, ownerId);
        }
        if (status != null && !status.isBlank()) {
            BookingStatus bookingStatus = parseBookingStatus(status);
            return bookingRepository.findByVehicleOwnerUserIdAndStatus(ownerId, bookingStatus);
        }
        return bookingRepository.findByVehicleOwnerUserId(ownerId);
    }

    @Override
    @Transactional
    public Booking confirmBooking(ConfirmBookingCommand command) {
        Booking booking = findBookingById(command.getBookingId());
        validateBookingBelongsToOwner(booking, command.getOwnerId());
        validateStatusEquals(booking, BookingStatus.PENDING, BookingErrorCode.ONLY_PENDING_CAN_BE_CONFIRMED);
        validateVehicleActive(booking.getVehicle());

        applyConfirmation(booking);
        Booking saved = bookingRepository.save(booking);

        autoRejectOverlappingPendingBookings(saved);
        return saved;
    }

    @Override
    @Transactional
    public Booking rejectBooking(RejectBookingCommand command) {
        Booking booking = findBookingById(command.getBookingId());
        validateBookingBelongsToOwner(booking, command.getOwnerId());
        validateStatusEquals(booking, BookingStatus.PENDING, BookingErrorCode.ONLY_PENDING_CAN_BE_REJECTED);
        validateReasonNotBlank(command.getReason(), BookingErrorCode.REJECTION_REASON_REQUIRED);

        booking.setStatus(BookingStatus.REJECTED);
        booking.setCancellationReason(command.getReason());
        return bookingRepository.save(booking);
    }

    // ── Admin operations ─────────────────────────────────────────────

    @Override
    public List<Booking> getAllBookings(String status, Long driverId, Long vehicleId) {
        BookingStatus bookingStatus = (status != null && !status.isBlank())
                ? parseBookingStatus(status) : null;
        return bookingRepository.findByFilters(bookingStatus, driverId, vehicleId);
    }

    @Override
    public Booking getBookingById(Long bookingId) {
        return findBookingById(bookingId);
    }

    // ── Photo operations ─────────────────────────────────────────────

    @Override
    public BookingPhoto getBookingPhoto(Long photoId) {
        return bookingPhotoRepository.findById(photoId)
                .orElseThrow(() -> new BusinessException(BookingErrorCode.BOOKING_NOT_FOUND));
    }

    // ── Scheduled tasks ──────────────────────────────────────────────

    /** Auto-cancels PENDING bookings whose startTime has passed (runs every 5 minutes). */
    @Scheduled(cron = BookingValidationConstraints.EVERY_FIVE_MINUTES_CRON)
    @Transactional
    public void autoCancelExpiredPendingBookings() {
        List<Booking> expired = bookingRepository.findPendingBookingsPastStartTime(LocalDateTime.now());
        expired.forEach(this::applySystemCancellation);
        if (!expired.isEmpty()) {
            bookingRepository.saveAll(expired);
            log.info("Auto-cancelled {} expired PENDING bookings", expired.size());
        }
    }

    /** Auto-completes IN_PROGRESS bookings that are more than 2 hours past their endTime (runs every 5 minutes). */
    @Scheduled(cron = BookingValidationConstraints.EVERY_FIVE_MINUTES_CRON)
    @Transactional
    public void autoCompleteOverdueInProgressBookings() {
        LocalDateTime graceCutoff = LocalDateTime.now()
                .minusHours(BookingValidationConstraints.AUTO_COMPLETE_GRACE_HOURS);
        List<Booking> overdue = bookingRepository.findInProgressBookingsPastGracePeriod(graceCutoff);
        overdue.forEach(b -> {
            b.setStatus(BookingStatus.COMPLETED);
            b.setCompletedAt(LocalDateTime.now());
        });
        if (!overdue.isEmpty()) {
            bookingRepository.saveAll(overdue);
            log.info("Auto-completed {} overdue IN_PROGRESS bookings", overdue.size());
        }
    }

    // ── Lookup helpers ───────────────────────────────────────────────

    /** Finds a booking by ID or throws BOOK-011. */
    private Booking findBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(BookingErrorCode.BOOKING_NOT_FOUND));
    }

    /** Finds a vehicle by ID or throws VEH-004. */
    private Vehicle findVehicleById(Long vehicleId) {
        return vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new BusinessException(VehicleErrorCode.VEHICLE_NOT_FOUND));
    }

    /** Finds a driver by ID or throws BOOK-024 if the driver does not exist. */
    private Driver findDriverById(Long driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException(BookingErrorCode.DRIVER_NOT_FOUND));
    }

    // ── Confirmation helpers ─────────────────────────────────────────

    /** Copies the vehicle's current location to the booking and sets confirmed state. */
    private void applyConfirmation(Booking booking) {
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());
        booking.setPickupLocation(booking.getVehicle().getGeneralLocation());
        booking.setPickupLatitude(booking.getVehicle().getLatitude());
        booking.setPickupLongitude(booking.getVehicle().getLongitude());
    }

    /** Auto-rejects PENDING bookings from the same driver that overlap the now-confirmed booking. */
    private void autoRejectOverlappingPendingBookings(Booking confirmedBooking) {
        List<Booking> overlapping = bookingRepository.findPendingOverlapsForDriver(
                confirmedBooking.getDriver().getUserId(),
                confirmedBooking.getBookingId(),
                confirmedBooking.getStartTime(),
                confirmedBooking.getEndTime()
        );
        overlapping.forEach(b -> {
            b.setStatus(BookingStatus.REJECTED);
            b.setCancellationReason(BookingValidationConstraints.AUTO_REJECT_REASON);
        });
        if (!overlapping.isEmpty()) {
            bookingRepository.saveAll(overlapping);
        }
    }

    // ── Cancellation helpers ─────────────────────────────────────────

    /** Applies cancellation fields to a booking. */
    private void applyCancellation(Booking booking, String reason, String cancelledBy) {
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledBy(cancelledBy);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason(reason);
    }

    /** Applies system-triggered cancellation (e.g., auto-cancel for expired PENDING). */
    private void applySystemCancellation(Booking booking) {
        applyCancellation(booking, BookingValidationConstraints.AUTO_CANCEL_REASON,
                BookingValidationConstraints.CANCELLED_BY_SYSTEM);
    }

    /** Maps the caller's role string to the cancelledBy identifier. */
    private String resolveCancelledByFromRole(String userRole) {
        return UserRole.DRIVER.name().equals(userRole)
                ? BookingValidationConstraints.CANCELLED_BY_DRIVER
                : BookingValidationConstraints.CANCELLED_BY_OWNER;
    }

    // ── Photo storage helpers ────────────────────────────────────────

    /**
     * Stores a booking photo using FileStorageService and returns a BookingPhoto entity.
     *
     * @param photo        the file to store
     * @param booking      the parent booking
     * @param subTypeDir   subdirectory name within the booking folder ("pickup" or "return")
     * @param photoTypeLabel the label stored in BookingPhoto.photoType ("PICKUP" or "RETURN")
     * @return a new BookingPhoto entity (not yet persisted)
     */
    private BookingPhoto storeBookingPhoto(MultipartFile photo, Booking booking,
                                           String subTypeDir, String photoTypeLabel) {
        Long bookingId = booking.getBookingId();
        Long driverId = booking.getDriver().getUserId();
        String subDirectory = BookingValidationConstraints.BOOKING_PHOTO_DIR + "/" + bookingId + "/" + subTypeDir;
        String storedUrl = fileStorageService.store(photo, driverId, subDirectory);

        String rawName = photo.getOriginalFilename() != null ? photo.getOriginalFilename() : "photo";
        String fileName = rawName.replaceAll(BookingValidationConstraints.UNSAFE_FILENAME_CHARS, "");
        if (fileName.length() > BookingValidationConstraints.MAX_FILENAME_LENGTH) {
            fileName = fileName.substring(0, BookingValidationConstraints.MAX_FILENAME_LENGTH);
        }
        return serviceMapper.toBookingPhoto(booking, photoTypeLabel, storedUrl, fileName, photo.getSize());
    }

    // ── Search response helpers ──────────────────────────────────────

    /** Builds a VehicleSearchResult with masked coordinates and effective service type. */
    private VehicleSearchResult buildVehicleSearchResponse(Vehicle vehicle, Driver driver) {
        double maskedLat = vehicle.getLatitude() != null
                ? locationMaskService.maskCoordinate(vehicle.getLatitude()) : 0.0;
        double maskedLng = vehicle.getLongitude() != null
                ? locationMaskService.maskCoordinate(vehicle.getLongitude()) : 0.0;
        String[] serviceTypeFields = computeServiceTypeFields(vehicle, driver);
        return serviceMapper.toVehicleSearchResult(vehicle, maskedLat, maskedLng, serviceTypeFields[0], serviceTypeFields[1]);
    }

    /** Builds a VehicleSearchResult for the detail view (same shape; mapper adds extra detail fields). */
    private VehicleSearchResult buildVehicleBookingDetailResponse(Vehicle vehicle, Driver driver) {
        return buildVehicleSearchResponse(vehicle, driver);
    }

    /**
     * Computes effectiveServiceType and serviceTypeWarning based on driver license and vehicle type.
     *
     * @return String[2] where [0] = effectiveServiceType, [1] = serviceTypeWarning (may be null)
     */
    private String[] computeServiceTypeFields(Vehicle vehicle, Driver driver) {
        ServiceType vehicleServiceType = vehicle.getServiceType();
        String driverLicenseClass = driver.getLicenseClass();

        if (vehicleServiceType == null || driverLicenseClass == null) {
            return new String[]{vehicleServiceType != null ? vehicleServiceType.name() : null, null};
        }

        boolean isClass5 = BookingValidationConstraints.LICENSE_CLASS_5.equalsIgnoreCase(driverLicenseClass);
        boolean isTaxiAndDelivery = vehicleServiceType == ServiceType.TAXI_AND_DELIVERY;

        if (isClass5 && isTaxiAndDelivery) {
            return new String[]{
                    ServiceType.DELIVERY_ONLY.name(),
                    BookingValidationConstraints.WARNING_CLASS5_TAXI
            };
        } else if (!isClass5 && vehicleServiceType == ServiceType.DELIVERY_ONLY) {
            return new String[]{
                    ServiceType.DELIVERY_ONLY.name(),
                    BookingValidationConstraints.WARNING_CLASS4_DELIVERY_ONLY
            };
        } else {
            return new String[]{vehicleServiceType.name(), null};
        }
    }

    // ── Geo-filter helper ────────────────────────────────────────────

    /** Returns true if the vehicle's availableUntil covers the requested endTime. */
    private boolean isAvailableUntilEndTime(Vehicle vehicle, LocalDateTime endTime) {
        if (endTime == null || vehicle.getAvailableUntil() == null) {
            return true;
        }
        return !vehicle.getAvailableUntil().isBefore(endTime);
    }

    /** Returns true if the vehicle passes the optional geo-radius filter from the command. */
    private boolean isWithinSearchRadius(Vehicle vehicle, SearchVehiclesCommand command) {
        if (command.getLatitude() == null || command.getLongitude() == null) {
            return true;
        }
        if (vehicle.getLatitude() == null || vehicle.getLongitude() == null) {
            return false;
        }
        double radius = command.getRadiusKm() != null
                ? command.getRadiusKm()
                : BookingValidationConstraints.DEFAULT_SEARCH_RADIUS_KM;
        return locationMaskService.isWithinRadius(
                vehicle.getLatitude(), vehicle.getLongitude(),
                command.getLatitude(), command.getLongitude(),
                radius);
    }

    // ── Conflict detection helpers ───────────────────────────────────

    /** Returns true if a conflicting booking exists for the vehicle in the given time range. */
    private boolean hasVehicleConflict(Long vehicleId, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return false;
        }
        return !bookingRepository.findConflictingBookings(vehicleId, CONFLICT_STATUSES, startTime, endTime).isEmpty();
    }

    // ── Enum parsing helpers ─────────────────────────────────────────

    private BookingStatus parseBookingStatus(String status) {
        try {
            return BookingStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(BookingErrorCode.BOOKING_NOT_FOUND);
        }
    }

    private VehicleCategory parseOptionalCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        try {
            return VehicleCategory.valueOf(category.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private ServiceType parseOptionalServiceType(String serviceType) {
        if (serviceType == null || serviceType.isBlank()) {
            return null;
        }
        try {
            return ServiceType.valueOf(serviceType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private FuelType parseOptionalFuelType(String fuelType) {
        if (fuelType == null || fuelType.isBlank()) {
            return null;
        }
        try {
            return FuelType.valueOf(fuelType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ── Business rule validations ────────────────────────────────────

    /** Validates the driver has isVerified == true (BOOK-001). */
    private void validateDriverIsVerified(Driver driver) {
        if (!Boolean.TRUE.equals(driver.getIsVerified())) {
            throw new BusinessException(BookingErrorCode.DRIVER_NOT_VERIFIED);
        }
    }

    /** Validates vehicle status is APPROVED (BOOK-002). */
    private void validateVehicleApproved(Vehicle vehicle) {
        if (vehicle.getStatus() != VehicleStatus.APPROVED) {
            throw new BusinessException(BookingErrorCode.VEHICLE_NOT_APPROVED);
        }
    }

    /** Validates vehicle is active (BOOK-003). */
    private void validateVehicleActive(Vehicle vehicle) {
        if (!Boolean.TRUE.equals(vehicle.getIsActive())) {
            throw new BusinessException(BookingErrorCode.VEHICLE_NOT_AVAILABLE);
        }
    }

    /** Validates endTime does not exceed the vehicle's availableUntil (BOOK-004). */
    private void validateEndTimeWithinAvailability(LocalDateTime endTime, Vehicle vehicle) {
        if (vehicle.getAvailableUntil() != null && endTime.isAfter(vehicle.getAvailableUntil())) {
            throw new BusinessException(BookingErrorCode.BOOKING_EXCEEDS_AVAILABILITY);
        }
    }

    /** Validates startTime is in the future (BOOK-005). */
    private void validateStartTimeInFuture(LocalDateTime startTime) {
        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new BusinessException(BookingErrorCode.START_TIME_NOT_FUTURE);
        }
    }

    /** Validates endTime is after startTime (BOOK-006). */
    private void validateEndTimeAfterStart(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException(BookingErrorCode.END_TIME_BEFORE_START);
        }
    }

    /** Validates minimum shift duration of 4 hours (BOOK-007). */
    private void validateMinShiftDuration(LocalDateTime startTime, LocalDateTime endTime) {
        long hours = Duration.between(startTime, endTime).toHours();
        if (hours < BookingValidationConstraints.MIN_SHIFT_HOURS) {
            throw new BusinessException(BookingErrorCode.MINIMUM_DURATION_NOT_MET);
        }
    }

    /** Validates maximum shift duration of 24 hours (BOOK-025). */
    private void validateMaxShiftDuration(LocalDateTime startTime, LocalDateTime endTime) {
        long hours = Duration.between(startTime, endTime).toHours();
        if (hours > BookingValidationConstraints.MAX_SHIFT_HOURS) {
            throw new BusinessException(BookingErrorCode.MAXIMUM_DURATION_EXCEEDED);
        }
    }

    /** Validates no existing conflicting bookings for the vehicle (BOOK-008). */
    private void validateNoVehicleConflict(Long vehicleId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                vehicleId, CONFLICT_STATUSES, startTime, endTime);
        if (!conflicts.isEmpty()) {
            throw new BusinessException(BookingErrorCode.VEHICLE_ALREADY_BOOKED);
        }
    }

    /** Validates the driver is not the owner of the vehicle (BOOK-009). */
    private void validateDriverNotOwner(Vehicle vehicle, Long driverId) {
        if (vehicle.getOwner().getUserId().equals(driverId)) {
            throw new BusinessException(BookingErrorCode.CANNOT_BOOK_OWN_VEHICLE);
        }
    }

    /** Validates the driver has no overlapping active bookings (BOOK-010). */
    private void validateNoDriverConflict(Long driverId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Booking> conflicts = bookingRepository.findDriverConflictingBookings(
                driverId,
                List.of(BookingStatus.CONFIRMED, BookingStatus.IN_PROGRESS),
                startTime, endTime);
        if (!conflicts.isEmpty()) {
            throw new BusinessException(BookingErrorCode.DRIVER_HAS_OVERLAPPING_BOOKING);
        }
    }

    /** Validates the requesting user has access to the booking (BOOK-012). */
    private void validateBookingAccess(Booking booking, Long userId, String userRole) {
        if (UserRole.DRIVER.name().equals(userRole)) {
            validateBookingBelongsToDriver(booking, userId);
        } else if (UserRole.CAR_OWNER.name().equals(userRole)) {
            validateBookingBelongsToOwner(booking, userId);
        }
    }

    /** Validates the booking belongs to the given driver (BOOK-012). */
    private void validateBookingBelongsToDriver(Booking booking, Long driverId) {
        if (!booking.getDriver().getUserId().equals(driverId)) {
            throw new BusinessException(BookingErrorCode.BOOKING_ACCESS_DENIED);
        }
    }

    /** Validates the booking's vehicle belongs to the given owner (BOOK-012). */
    private void validateBookingBelongsToOwner(Booking booking, Long ownerId) {
        if (!booking.getVehicle().getOwner().getUserId().equals(ownerId)) {
            throw new BusinessException(BookingErrorCode.BOOKING_ACCESS_DENIED);
        }
    }

    /** Validates that the booking status matches the expected status. */
    private void validateStatusEquals(Booking booking, BookingStatus expected, BookingErrorCode errorCode) {
        if (booking.getStatus() != expected) {
            throw new BusinessException(errorCode);
        }
    }

    /** Validates PENDING or CONFIRMED is allowed for cancellation (BOOK-015), respecting owner role. */
    private void validateCancellationStatusAllowed(Booking booking, String userRole) {
        BookingStatus status = booking.getStatus();
        boolean isOwner = UserRole.CAR_OWNER.name().equals(userRole);
        if (isOwner) {
            if (status != BookingStatus.CONFIRMED) {
                throw new BusinessException(BookingErrorCode.INVALID_STATUS_FOR_CANCELLATION);
            }
        } else {
            if (status != BookingStatus.PENDING && status != BookingStatus.CONFIRMED) {
                throw new BusinessException(BookingErrorCode.INVALID_STATUS_FOR_CANCELLATION);
            }
        }
    }

    /** Validates the start window: current time must not be more than 15 min early (BOOK-017). */
    private void validateStartWindowNotTooEarly(Booking booking) {
        LocalDateTime earliestStart = booking.getStartTime()
                .minusMinutes(BookingValidationConstraints.EARLY_START_MINUTES);
        if (LocalDateTime.now().isBefore(earliestStart)) {
            throw new BusinessException(BookingErrorCode.START_TOO_EARLY);
        }
    }

    /** Validates the start window: current time must not be more than 1 hour after startTime (BOOK-018). */
    private void validateStartWindowNotExpired(Booking booking) {
        LocalDateTime latestStart = booking.getStartTime()
                .plusHours(BookingValidationConstraints.LATE_START_HOURS);
        if (LocalDateTime.now().isAfter(latestStart)) {
            throw new BusinessException(BookingErrorCode.START_WINDOW_EXPIRED);
        }
    }

    /** Validates booking is CONFIRMED/IN_PROGRESS/COMPLETED to access vehicle location (BOOK-016). */
    private void validateBookingIsConfirmedOrLater(Booking booking) {
        BookingStatus status = booking.getStatus();
        if (status != BookingStatus.CONFIRMED
                && status != BookingStatus.IN_PROGRESS
                && status != BookingStatus.COMPLETED) {
            throw new BusinessException(BookingErrorCode.ONLY_CONFIRMED_CAN_BE_STARTED);
        }
    }

    /** Validates a reason string is not blank. */
    private void validateReasonNotBlank(String reason, BookingErrorCode errorCode) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(errorCode);
        }
    }

    /** Validates that the photo list is not null/empty (BOOK-020 / BOOK-021). */
    private void validatePhotosPresent(List<MultipartFile> photos, BookingErrorCode errorCode) {
        if (photos == null || photos.isEmpty()) {
            throw new BusinessException(errorCode);
        }
    }

    /** Validates that the photo list does not exceed the maximum allowed count (BOOK-026). */
    private void validatePhotoCount(List<MultipartFile> photos) {
        if (photos.size() > BookingValidationConstraints.MAX_PHOTOS_PER_UPLOAD) {
            throw new BusinessException(BookingErrorCode.TOO_MANY_PHOTOS);
        }
    }

    /** Validates photo extension is one of: jpg, jpeg, png (DOC-001). */
    private void validatePhotoFormat(MultipartFile photo) {
        String originalFilename = photo.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new BusinessException(DocumentErrorCode.INVALID_FILE_FORMAT);
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!BookingValidationConstraints.ALLOWED_PHOTO_EXTENSIONS.contains(extension)) {
            throw new BusinessException(DocumentErrorCode.INVALID_FILE_FORMAT);
        }
    }

    /** Validates photo size does not exceed 5 MB (DOC-002). */
    private void validatePhotoSize(MultipartFile photo) {
        if (photo.getSize() > BookingValidationConstraints.MAX_PHOTO_SIZE_BYTES) {
            throw new BusinessException(DocumentErrorCode.FILE_TOO_LARGE);
        }
    }
}
