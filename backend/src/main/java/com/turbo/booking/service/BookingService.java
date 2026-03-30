package com.turbo.booking.service;

import com.turbo.booking.model.Booking;
import com.turbo.booking.model.BookingPhoto;
import com.turbo.booking.service.command.CancelBookingCommand;
import com.turbo.booking.service.command.CompleteBookingCommand;
import com.turbo.booking.service.command.ConfirmBookingCommand;
import com.turbo.booking.service.command.CreateBookingCommand;
import com.turbo.booking.service.command.GetBookingCommand;
import com.turbo.booking.service.command.RejectBookingCommand;
import com.turbo.booking.service.command.SearchVehiclesCommand;
import com.turbo.booking.service.command.StartBookingCommand;
import com.turbo.booking.service.result.DriverHoursSummary;
import com.turbo.booking.service.result.LocationResult;
import com.turbo.booking.service.result.OwnerDashboardStats;
import com.turbo.booking.service.result.VehicleSearchResult;

import java.util.List;

public interface BookingService {

    // ── Driver operations ────────────────────────────────────────────

    DriverHoursSummary getDriverHoursSummary(Long driverId);

    List<VehicleSearchResult> searchAvailableVehicles(SearchVehiclesCommand command);

    VehicleSearchResult getVehicleDetail(Long vehicleId, Long driverId);

    Booking createBooking(CreateBookingCommand command);

    List<Booking> getDriverBookings(Long driverId, String status);

    Booking getBookingForUser(GetBookingCommand command);

    Booking cancelBooking(CancelBookingCommand command);

    Booking startBooking(StartBookingCommand command);

    Booking completeBooking(CompleteBookingCommand command);

    LocationResult getVehicleLocation(GetBookingCommand command);

    // ── Owner operations ─────────────────────────────────────────────

    OwnerDashboardStats getOwnerDashboardStats(Long ownerId);

    List<Booking> getOwnerBookings(Long ownerId, String status, Long vehicleId);

    Booking confirmBooking(ConfirmBookingCommand command);

    Booking rejectBooking(RejectBookingCommand command);

    // ── Admin operations ─────────────────────────────────────────────

    List<Booking> getAllBookings(String status, Long driverId, Long vehicleId);

    Booking getBookingById(Long bookingId);

    // ── Photo operations ─────────────────────────────────────────────

    BookingPhoto getBookingPhoto(Long photoId);
}
