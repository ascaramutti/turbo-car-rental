package com.turbo.booking.controller.mapper;

import com.turbo.booking.dto.BookingDetailResponse;
import com.turbo.booking.dto.BookingResponse;
import com.turbo.booking.dto.CreateBookingRequest;
import com.turbo.booking.dto.DriverHoursSummaryResponse;
import com.turbo.booking.dto.VehicleBookingDetailResponse;
import com.turbo.booking.dto.VehicleLocationResponse;
import com.turbo.booking.dto.VehicleSearchResponse;
import com.turbo.booking.model.Booking;
import com.turbo.booking.service.command.CancelBookingCommand;
import com.turbo.booking.service.command.CompleteBookingCommand;
import com.turbo.booking.service.command.CreateBookingCommand;
import com.turbo.booking.service.command.GetBookingCommand;
import com.turbo.booking.service.command.SearchVehiclesCommand;
import com.turbo.booking.service.command.StartBookingCommand;
import com.turbo.booking.service.result.DriverHoursSummary;
import com.turbo.booking.service.result.LocationResult;
import com.turbo.booking.service.result.VehicleSearchResult;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = "spring")
public interface DriverBookingControllerMapper {

    // ── Request → Command mappings ──────────────────────────────────

    @Mapping(source = "driverId", target = "driverId")
    @Mapping(source = "request.vehicleId", target = "vehicleId")
    @Mapping(source = "request.startTime", target = "startTime")
    @Mapping(source = "request.endTime", target = "endTime")
    CreateBookingCommand toCreateCommand(CreateBookingRequest request, Long driverId);

    @Mapping(source = "driverId", target = "driverId")
    @Mapping(source = "radiusKm", target = "radiusKm")
    @Mapping(source = "startTime", target = "startTime")
    @Mapping(source = "endTime", target = "endTime")
    @Mapping(source = "category", target = "category")
    @Mapping(source = "serviceType", target = "serviceType")
    @Mapping(source = "minPrice", target = "minPrice")
    @Mapping(source = "maxPrice", target = "maxPrice")
    @Mapping(source = "fuelType", target = "fuelType")
    SearchVehiclesCommand toSearchCommand(Long driverId, Double latitude, Double longitude,
                                          Double radiusKm, LocalDateTime startTime, LocalDateTime endTime,
                                          String category, String serviceType, BigDecimal minPrice,
                                          BigDecimal maxPrice, String fuelType);

    @Mapping(source = "bookingId", target = "bookingId")
    @Mapping(source = "driverId", target = "userId")
    @Mapping(target = "userRole", constant = "DRIVER")
    GetBookingCommand toGetCommand(Long bookingId, Long driverId);

    @Mapping(source = "bookingId", target = "bookingId")
    @Mapping(source = "driverId", target = "userId")
    @Mapping(source = "reason", target = "reason")
    @Mapping(target = "userRole", constant = "DRIVER")
    CancelBookingCommand toCancelCommand(Long bookingId, Long driverId, String reason);

    @Mapping(source = "bookingId", target = "bookingId")
    @Mapping(source = "driverId", target = "driverId")
    @Mapping(source = "pickupPhotos", target = "pickupPhotos")
    StartBookingCommand toStartCommand(Long bookingId, Long driverId, List<MultipartFile> pickupPhotos);

    @Mapping(source = "bookingId", target = "bookingId")
    @Mapping(source = "driverId", target = "driverId")
    @Mapping(source = "returnPhotos", target = "returnPhotos")
    CompleteBookingCommand toCompleteCommand(Long bookingId, Long driverId, List<MultipartFile> returnPhotos);

    // ── Entity → Response mappings ──────────────────────────────────

    @Named("toBookingResponse")
    @Mapping(source = "driver.userId", target = "driverId")
    @Mapping(target = "driverFullName", expression = "java(booking.getDriver().getFirstName() + \" \" + booking.getDriver().getLastName())")
    @Mapping(source = "vehicle.vehicleId", target = "vehicleId")
    @Mapping(target = "vehicleSummary", expression = "java(booking.getVehicle().getYear() + \" \" + booking.getVehicle().getMake() + \" \" + booking.getVehicle().getModel())")
    @Mapping(target = "status", expression = "java(booking.getStatus().name())")
    @Mapping(target = "startTime", expression = "java(booking.getStartTime().toString())")
    @Mapping(target = "endTime", expression = "java(booking.getEndTime().toString())")
    @Mapping(target = "createdAt", expression = "java(booking.getCreatedAt().toString())")
    @Mapping(target = "updatedAt", expression = "java(booking.getUpdatedAt().toString())")
    BookingResponse toBookingResponse(Booking booking);

    @IterableMapping(qualifiedByName = "toBookingResponse")
    List<BookingResponse> toBookingResponseList(List<Booking> bookings);

    @Mapping(source = "driver.userId", target = "driverId")
    @Mapping(target = "driverFullName", expression = "java(booking.getDriver().getFirstName() + \" \" + booking.getDriver().getLastName())")
    @Mapping(source = "vehicle.vehicleId", target = "vehicleId")
    @Mapping(target = "vehicleSummary", expression = "java(booking.getVehicle().getYear() + \" \" + booking.getVehicle().getMake() + \" \" + booking.getVehicle().getModel())")
    @Mapping(target = "status", expression = "java(booking.getStatus().name())")
    @Mapping(target = "startTime", expression = "java(booking.getStartTime().toString())")
    @Mapping(target = "endTime", expression = "java(booking.getEndTime().toString())")
    @Mapping(target = "createdAt", expression = "java(booking.getCreatedAt().toString())")
    @Mapping(target = "updatedAt", expression = "java(booking.getUpdatedAt().toString())")
    @Mapping(source = "vehicle.owner.userId", target = "ownerId")
    @Mapping(target = "ownerFullName", expression = "java(booking.getVehicle().getOwner().getFirstName() + \" \" + booking.getVehicle().getOwner().getLastName())")
    @Mapping(source = "vehicle.make", target = "vehicleMake")
    @Mapping(source = "vehicle.model", target = "vehicleModel")
    @Mapping(source = "vehicle.year", target = "vehicleYear")
    @Mapping(source = "vehicle.licensePlate", target = "vehicleLicensePlate")
    @Mapping(target = "vehicleCategory", expression = "java(booking.getVehicle().getCategory().name())")
    @Mapping(target = "vehicleServiceType", expression = "java(booking.getVehicle().getServiceType() != null ? booking.getVehicle().getServiceType().name() : null)")
    @Mapping(source = "vehicle.hourlyRate", target = "vehicleHourlyRate")
    @Mapping(target = "confirmedAt", expression = "java(booking.getConfirmedAt() != null ? booking.getConfirmedAt().toString() : null)")
    @Mapping(target = "startedAt", expression = "java(booking.getStartedAt() != null ? booking.getStartedAt().toString() : null)")
    @Mapping(target = "completedAt", expression = "java(booking.getCompletedAt() != null ? booking.getCompletedAt().toString() : null)")
    @Mapping(target = "cancelledAt", expression = "java(booking.getCancelledAt() != null ? booking.getCancelledAt().toString() : null)")
    @Mapping(target = "pickupPhotoUrls", expression = "java(booking.getPhotos().stream().filter(p -> com.turbo.booking.validation.BookingValidationConstraints.PHOTO_TYPE_PICKUP.equals(p.getPhotoType())).map(p -> \"/bookings/photos/\" + p.getPhotoId()).collect(java.util.stream.Collectors.toList()))")
    @Mapping(target = "returnPhotoUrls", expression = "java(booking.getPhotos().stream().filter(p -> com.turbo.booking.validation.BookingValidationConstraints.PHOTO_TYPE_RETURN.equals(p.getPhotoType())).map(p -> \"/bookings/photos/\" + p.getPhotoId()).collect(java.util.stream.Collectors.toList()))")
    @Mapping(target = "effectiveServiceType", ignore = true)
    @Mapping(target = "serviceTypeWarning", ignore = true)
    BookingDetailResponse toBookingDetailResponse(Booking booking);

    // ── VehicleSearchResult → DTO mappings ───────────────────────────

    @Named("toVehicleSearchResponse")
    @Mapping(source = "vehicle.vehicleId", target = "vehicleId")
    @Mapping(source = "vehicle.make", target = "make")
    @Mapping(source = "vehicle.model", target = "model")
    @Mapping(source = "vehicle.year", target = "year")
    @Mapping(target = "category", expression = "java(result.getVehicle().getCategory().name())")
    @Mapping(target = "fuelType", expression = "java(result.getVehicle().getFuelType().name())")
    @Mapping(target = "serviceType", expression = "java(result.getVehicle().getServiceType() != null ? result.getVehicle().getServiceType().name() : null)")
    @Mapping(source = "vehicle.hourlyRate", target = "hourlyRate")
    @Mapping(source = "vehicle.description", target = "description")
    @Mapping(source = "vehicle.generalLocation", target = "generalLocation")
    @Mapping(target = "maskedLatitude", expression = "java(result.getVehicle().getLatitude() != null ? result.getMaskedLatitude() : null)")
    @Mapping(target = "maskedLongitude", expression = "java(result.getVehicle().getLongitude() != null ? result.getMaskedLongitude() : null)")
    @Mapping(target = "availableUntil", expression = "java(result.getVehicle().getAvailableUntil() != null ? result.getVehicle().getAvailableUntil().toString() : null)")
    @Mapping(target = "ownerFullName", expression = "java(result.getVehicle().getOwner().getFirstName() + \" \" + result.getVehicle().getOwner().getLastName())")
    @Mapping(source = "vehicle.owner.rating", target = "ownerRating")
    @Mapping(source = "effectiveServiceType", target = "effectiveServiceType")
    @Mapping(source = "serviceTypeWarning", target = "serviceTypeWarning")
    VehicleSearchResponse toVehicleSearchResponse(VehicleSearchResult result);

    @IterableMapping(qualifiedByName = "toVehicleSearchResponse")
    List<VehicleSearchResponse> toVehicleSearchResponseList(List<VehicleSearchResult> results);

    @Mapping(source = "vehicle.vehicleId", target = "vehicleId")
    @Mapping(source = "vehicle.make", target = "make")
    @Mapping(source = "vehicle.model", target = "model")
    @Mapping(source = "vehicle.year", target = "year")
    @Mapping(target = "category", expression = "java(result.getVehicle().getCategory().name())")
    @Mapping(target = "fuelType", expression = "java(result.getVehicle().getFuelType().name())")
    @Mapping(target = "serviceType", expression = "java(result.getVehicle().getServiceType() != null ? result.getVehicle().getServiceType().name() : null)")
    @Mapping(source = "vehicle.hourlyRate", target = "hourlyRate")
    @Mapping(source = "vehicle.description", target = "description")
    @Mapping(source = "vehicle.generalLocation", target = "generalLocation")
    @Mapping(target = "maskedLatitude", expression = "java(result.getVehicle().getLatitude() != null ? result.getMaskedLatitude() : null)")
    @Mapping(target = "maskedLongitude", expression = "java(result.getVehicle().getLongitude() != null ? result.getMaskedLongitude() : null)")
    @Mapping(target = "availableUntil", expression = "java(result.getVehicle().getAvailableUntil() != null ? result.getVehicle().getAvailableUntil().toString() : null)")
    @Mapping(target = "ownerFullName", expression = "java(result.getVehicle().getOwner().getFirstName() + \" \" + result.getVehicle().getOwner().getLastName())")
    @Mapping(source = "vehicle.owner.rating", target = "ownerRating")
    @Mapping(source = "effectiveServiceType", target = "effectiveServiceType")
    @Mapping(source = "serviceTypeWarning", target = "serviceTypeWarning")
    @Mapping(source = "vehicle.licensePlate", target = "licensePlate")
    @Mapping(source = "vehicle.owner.userId", target = "ownerId")
    @Mapping(source = "vehicle.vin", target = "vin")
    VehicleBookingDetailResponse toVehicleBookingDetailResponse(VehicleSearchResult result);

    // ── LocationResult → DTO ─────────────────────────────────────────

    @Mapping(source = "vehicleId", target = "vehicleId")
    @Mapping(source = "exactLocation", target = "isExactLocation")
    @Mapping(source = "generalLocation", target = "generalLocation")
    @Mapping(source = "latitude", target = "latitude")
    @Mapping(source = "longitude", target = "longitude")
    @Mapping(source = "message", target = "message")
    VehicleLocationResponse toVehicleLocationResponse(LocationResult result);

    // ── DriverHoursSummary → DTO ─────────────────────────────────────

    @Mapping(source = "hoursUsedThisWeek", target = "hoursUsedThisWeek")
    @Mapping(source = "maxHoursPerWeek", target = "maxHoursPerWeek")
    @Mapping(source = "hoursRemaining", target = "hoursRemaining")
    DriverHoursSummaryResponse toDriverHoursSummaryResponse(DriverHoursSummary summary);
}
