package com.turbo.booking.service.mapper;

import com.turbo.booking.model.Booking;
import com.turbo.booking.model.BookingPhoto;
import com.turbo.booking.service.command.CreateBookingCommand;
import com.turbo.booking.service.result.DriverHoursSummary;
import com.turbo.booking.service.result.LocationResult;
import com.turbo.booking.service.result.OwnerDashboardStats;
import com.turbo.booking.service.result.VehicleSearchResult;
import com.turbo.user.model.Driver;
import com.turbo.vehicle.model.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface BookingServiceMapper {

    // ── Booking entity ───────────────────────────────────────────────

    @Mapping(source = "command.startTime", target = "startTime")
    @Mapping(source = "command.endTime", target = "endTime")
    @Mapping(source = "driver", target = "driver")
    @Mapping(source = "vehicle", target = "vehicle")
    @Mapping(source = "totalHours", target = "totalHours")
    @Mapping(source = "totalPrice", target = "totalPrice")
    @Mapping(target = "status", expression = "java(com.turbo.booking.model.enums.BookingStatus.PENDING)")
    @Mapping(target = "bookingId", ignore = true)
    @Mapping(target = "pickupLocation", ignore = true)
    @Mapping(target = "pickupLatitude", ignore = true)
    @Mapping(target = "pickupLongitude", ignore = true)
    @Mapping(target = "photos", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "cancelledBy", ignore = true)
    @Mapping(target = "confirmedAt", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Booking toBooking(CreateBookingCommand command, Driver driver, Vehicle vehicle,
                      int totalHours, BigDecimal totalPrice);

    // ── BookingPhoto entity ──────────────────────────────────────────

    @Mapping(source = "booking", target = "booking")
    @Mapping(source = "photoType", target = "photoType")
    @Mapping(source = "fileUrl", target = "fileUrl")
    @Mapping(source = "fileName", target = "fileName")
    @Mapping(source = "fileSize", target = "fileSize")
    @Mapping(target = "photoId", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    BookingPhoto toBookingPhoto(Booking booking, String photoType,
                                String fileUrl, String fileName, Long fileSize);

    // ── VehicleSearchResult ──────────────────────────────────────────

    @Mapping(source = "vehicle", target = "vehicle")
    @Mapping(source = "maskedLat", target = "maskedLatitude")
    @Mapping(source = "maskedLng", target = "maskedLongitude")
    @Mapping(source = "effectiveServiceType", target = "effectiveServiceType")
    @Mapping(source = "serviceTypeWarning", target = "serviceTypeWarning")
    VehicleSearchResult toVehicleSearchResult(Vehicle vehicle, Double maskedLat, Double maskedLng,
                                              String effectiveServiceType, String serviceTypeWarning);

    // ── LocationResult ───────────────────────────────────────────────

    @Mapping(source = "vehicleId", target = "vehicleId")
    @Mapping(source = "exactLocation", target = "exactLocation")
    @Mapping(source = "generalLocation", target = "generalLocation")
    @Mapping(source = "latitude", target = "latitude")
    @Mapping(source = "longitude", target = "longitude")
    @Mapping(source = "message", target = "message")
    LocationResult toLocationResult(Long vehicleId, boolean exactLocation, String generalLocation,
                                    Double latitude, Double longitude, String message);

    // ── DriverHoursSummary ───────────────────────────────────────────

    @Mapping(source = "hoursUsed", target = "hoursUsedThisWeek")
    @Mapping(source = "maxHours", target = "maxHoursPerWeek")
    @Mapping(source = "remaining", target = "hoursRemaining")
    DriverHoursSummary toDriverHoursSummary(Integer hoursUsed, Integer maxHours, Integer remaining);

    // ── OwnerDashboardStats ──────────────────────────────────────────

    @Mapping(source = "activeVehicles", target = "activeVehicles")
    @Mapping(source = "totalEarnings", target = "totalEarnings")
    @Mapping(source = "monthEarnings", target = "monthEarnings")
    @Mapping(source = "completedBookings", target = "completedBookings")
    @Mapping(source = "rating", target = "rating")
    OwnerDashboardStats toOwnerDashboardStats(Integer activeVehicles, BigDecimal totalEarnings,
                                              BigDecimal monthEarnings, Integer completedBookings,
                                              Float rating);
}
