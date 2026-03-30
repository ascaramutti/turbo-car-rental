package com.turbo.booking.controller.mapper;

import com.turbo.booking.dto.BookingDetailResponse;
import com.turbo.booking.dto.BookingResponse;
import com.turbo.booking.model.Booking;
import com.turbo.booking.service.command.CancelBookingCommand;
import com.turbo.booking.service.command.ConfirmBookingCommand;
import com.turbo.booking.service.command.GetBookingCommand;
import com.turbo.booking.service.command.RejectBookingCommand;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OwnerBookingControllerMapper {

    // ── Request → Command mappings ──────────────────────────────────

    @Mapping(source = "bookingId", target = "bookingId")
    @Mapping(source = "ownerId", target = "ownerId")
    ConfirmBookingCommand toConfirmCommand(Long bookingId, Long ownerId);

    @Mapping(source = "bookingId", target = "bookingId")
    @Mapping(source = "ownerId", target = "ownerId")
    @Mapping(source = "reason", target = "reason")
    RejectBookingCommand toRejectCommand(Long bookingId, Long ownerId, String reason);

    @Mapping(source = "bookingId", target = "bookingId")
    @Mapping(source = "ownerId", target = "userId")
    @Mapping(source = "reason", target = "reason")
    @Mapping(target = "userRole", constant = "CAR_OWNER")
    CancelBookingCommand toCancelCommand(Long bookingId, Long ownerId, String reason);

    @Mapping(source = "bookingId", target = "bookingId")
    @Mapping(source = "ownerId", target = "userId")
    @Mapping(target = "userRole", constant = "CAR_OWNER")
    GetBookingCommand toGetCommand(Long bookingId, Long ownerId);

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
}
