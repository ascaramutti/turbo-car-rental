package com.turbo.booking.controller.mapper;

import com.turbo.booking.dto.AdminBookingDetailResponse;
import com.turbo.booking.dto.AdminBookingResponse;
import com.turbo.booking.model.Booking;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminBookingControllerMapper {

    // ── Entity → Response mappings ──────────────────────────────────

    @Named("toAdminBookingResponse")
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
    @Mapping(source = "vehicle.licensePlate", target = "vehicleLicensePlate")
    AdminBookingResponse toAdminBookingResponse(Booking booking);

    @IterableMapping(qualifiedByName = "toAdminBookingResponse")
    List<AdminBookingResponse> toAdminBookingResponseList(List<Booking> bookings);

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
    @Mapping(source = "vehicle.licensePlate", target = "vehicleLicensePlate")
    @Mapping(source = "vehicle.make", target = "vehicleMake")
    @Mapping(source = "vehicle.model", target = "vehicleModel")
    @Mapping(source = "vehicle.year", target = "vehicleYear")
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
    AdminBookingDetailResponse toAdminBookingDetailResponse(Booking booking);
}
