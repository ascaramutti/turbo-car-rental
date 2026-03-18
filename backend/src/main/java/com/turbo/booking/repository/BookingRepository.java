package com.turbo.booking.repository;

import com.turbo.booking.model.Booking;
import com.turbo.booking.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ── Driver queries ───────────────────────────────────────────────

    List<Booking> findByDriverUserId(Long driverId);

    List<Booking> findByDriverUserIdAndStatus(Long driverId, BookingStatus status);

    // ── Owner queries ────────────────────────────────────────────────

    List<Booking> findByVehicleOwnerUserId(Long ownerId);

    List<Booking> findByVehicleOwnerUserIdAndStatus(Long ownerId, BookingStatus status);

    List<Booking> findByVehicleVehicleIdAndVehicleOwnerUserId(Long vehicleId, Long ownerId);

    List<Booking> findByVehicleVehicleIdAndVehicleOwnerUserIdAndStatus(Long vehicleId, Long ownerId, BookingStatus status);

    // ── Conflict detection ───────────────────────────────────────────

    /** Returns bookings for the given vehicle that overlap the given time range and match one of the given statuses. */
    @Query("SELECT b FROM Booking b WHERE b.vehicle.vehicleId = :vehicleId " +
           "AND b.status IN :activeStatuses " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    List<Booking> findConflictingBookings(
            @Param("vehicleId") Long vehicleId,
            @Param("activeStatuses") List<BookingStatus> activeStatuses,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /** Returns bookings for the given driver that overlap the given time range and match one of the given statuses. */
    @Query("SELECT b FROM Booking b WHERE b.driver.userId = :driverId " +
           "AND b.status IN :activeStatuses " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    List<Booking> findDriverConflictingBookings(
            @Param("driverId") Long driverId,
            @Param("activeStatuses") List<BookingStatus> activeStatuses,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /** Returns PENDING bookings for a driver that overlap the confirmed booking's time range (excluding the confirmed booking itself). */
    @Query("SELECT b FROM Booking b WHERE b.driver.userId = :driverId " +
           "AND b.status = com.turbo.booking.model.enums.BookingStatus.PENDING " +
           "AND b.bookingId != :excludeBookingId " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    List<Booking> findPendingOverlapsForDriver(
            @Param("driverId") Long driverId,
            @Param("excludeBookingId") Long excludeBookingId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // ── Scheduled task queries ───────────────────────────────────────

    /** Returns PENDING bookings whose startTime has already passed (eligible for auto-cancellation). */
    @Query("SELECT b FROM Booking b WHERE b.status = com.turbo.booking.model.enums.BookingStatus.PENDING AND b.startTime <= :now")
    List<Booking> findPendingBookingsPastStartTime(@Param("now") LocalDateTime now);

    /** Returns IN_PROGRESS bookings whose endTime plus the grace period has passed (eligible for auto-completion). */
    @Query("SELECT b FROM Booking b WHERE b.status = com.turbo.booking.model.enums.BookingStatus.IN_PROGRESS AND b.endTime <= :graceCutoff")
    List<Booking> findInProgressBookingsPastGracePeriod(@Param("graceCutoff") LocalDateTime graceCutoff);

    // ── Admin queries ────────────────────────────────────────────────

    @Query("SELECT b FROM Booking b WHERE (:status IS NULL OR b.status = :status) " +
           "AND (:driverId IS NULL OR b.driver.userId = :driverId) " +
           "AND (:vehicleId IS NULL OR b.vehicle.vehicleId = :vehicleId)")
    List<Booking> findByFilters(
            @Param("status") BookingStatus status,
            @Param("driverId") Long driverId,
            @Param("vehicleId") Long vehicleId
    );
}
